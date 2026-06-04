package com.undernine.utils.biz.importtask;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvImportRowReaderTest {

    @Test
    void read_headerMapping() throws Exception {
        List<CsvRow> rows = readRows(CsvImportRowReader.builder(new StringReader("""
                name,quantity
                apple,10
                banana,20
                """))
                .hasHeader(true)
                .build());

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().getRowNumber()).isEqualTo(2);
        assertThat(rows.getFirst().getHeaders()).containsExactly("name", "quantity");
        assertThat(rows.getFirst().containsHeader("name")).isTrue();
        assertThat(rows.getFirst().get("name")).isEqualTo("apple");
        assertThat(rows.getFirst().get("quantity")).isEqualTo("10");
        assertThat(rows.getFirst().get(0)).isEqualTo("apple");
        assertThat(rows.getFirst().get(2)).isNull();
    }

    @Test
    void read_quotedCommaAndEscapedQuote() throws Exception {
        List<CsvRow> rows = readRows(CsvImportRowReader.builder(new StringReader(
                "name,description\n\"apple, red\",\"He said \"\"fresh\"\"\"\n"))
                .hasHeader(true)
                .build());

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("name")).isEqualTo("apple, red");
            assertThat(row.get("description")).isEqualTo("He said \"fresh\"");
        });
    }

    @Test
    void read_customDelimiter() throws Exception {
        List<CsvRow> rows = readRows(CsvImportRowReader.builder(new StringReader("""
                name;quantity
                apple;10
                """))
                .hasHeader(true)
                .delimiter(';')
                .build());

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("name")).isEqualTo("apple");
            assertThat(row.get("quantity")).isEqualTo("10");
        });
    }

    @Test
    void read_utf8BomHeader() throws Exception {
        List<CsvRow> rows = readRows(CsvImportRowReader.builder(new StringReader(
                "\ufeffname,quantity\napple,10\n"))
                .hasHeader(true)
                .build());

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getHeaders()).containsExactly("name", "quantity");
            assertThat(row.get("name")).isEqualTo("apple");
        });
    }

    @Test
    void read_rejectsUnclosedQuotedFieldInStrictMode() {
        CsvImportRowReader reader = CsvImportRowReader.builder(new StringReader(
                "name,description\napple,\"fresh\n"))
                .hasHeader(true)
                .build();

        assertThatThrownBy(() -> readRows(reader))
                .isInstanceOf(ImportTaskException.class)
                .hasMessageContaining("Malformed CSV record at line 2")
                .hasMessageContaining("quoted field is not closed");
    }

    @Test
    void read_rejectsUnexpectedCharacterAfterClosingQuoteInStrictMode() {
        CsvImportRowReader reader = CsvImportRowReader.builder(new StringReader(
                "name,description\n\"apple\"bad,10\n"))
                .hasHeader(true)
                .build();

        assertThatThrownBy(() -> readRows(reader))
                .isInstanceOf(ImportTaskException.class)
                .hasMessageContaining("unexpected character after closing quote");
    }

    @Test
    void read_supportsLegacyLooseQuoteParsingWhenStrictModeDisabled() throws Exception {
        List<CsvRow> rows = readRows(CsvImportRowReader.builder(new StringReader(
                "name,description\n\"apple\"bad,10\n"))
                .hasHeader(true)
                .strictQuotes(false)
                .build());

        assertThat(rows).singleElement().satisfies(row ->
                assertThat(row.get("name")).isEqualTo("applebad"));
    }

    @Test
    void read_rejectsOversizedRecord() {
        CsvImportRowReader reader = CsvImportRowReader.builder(new StringReader(
                "name\napple\n"))
                .hasHeader(true)
                .maxRecordChars(3)
                .build();

        assertThatThrownBy(() -> readRows(reader))
                .isInstanceOf(ImportTaskException.class)
                .hasMessageContaining("record length exceeds 3 characters");
    }

    @Test
    void read_blankRowPolicyKeepsAndSkipsBlankRows() throws Exception {
        String csv = """
                name,quantity

                 ,
                apple,10
                """;

        List<CsvRow> keptRows = readRows(CsvImportRowReader.builder(new StringReader(csv))
                .hasHeader(true)
                .blankRowPolicy(CsvImportRowReader.BlankRowPolicy.KEEP)
                .build());
        List<CsvRow> skippedRows = readRows(CsvImportRowReader.builder(new StringReader(csv))
                .hasHeader(true)
                .blankRowPolicy(CsvImportRowReader.BlankRowPolicy.SKIP)
                .build());

        assertThat(keptRows).hasSize(3);
        assertThat(keptRows.get(0).isBlank()).isTrue();
        assertThat(keptRows.get(0).getRowNumber()).isEqualTo(2);
        assertThat(keptRows.get(1).isBlank()).isTrue();
        assertThat(keptRows.get(1).getRowNumber()).isEqualTo(3);
        assertThat(skippedRows).singleElement().satisfies(row -> {
            assertThat(row.getRowNumber()).isEqualTo(4);
            assertThat(row.get("name")).isEqualTo("apple");
        });
    }

    @Test
    void execute_closesImportRowReader() {
        CloseTrackingReader source = new CloseTrackingReader("""
                name,quantity
                apple,10
                """);
        CsvImportRowReader rowReader = CsvImportRowReader.builder(source)
                .hasHeader(true)
                .build();
        ImportTaskTemplate template = ImportTaskTemplate.create();

        ImportResult result = template.execute(rowReader, new PassthroughCsvRowHandler(new ArrayList<>()));

        assertThat(result.isAllSuccess()).isTrue();
        assertThat(source.isClosed()).isTrue();
    }

    @Test
    void execute_csvReaderIntegratesWithTemplateSuccessAndFailure() {
        List<Item> processedRows = new ArrayList<>();
        CsvImportRowReader rowReader = CsvImportRowReader.builder(new StringReader("""
                name,quantity
                apple,10
                ,0
                boom,1
                banana,20
                """))
                .hasHeader(true)
                .build();
        ImportTaskTemplate template = ImportTaskTemplate.create();

        ImportResult result = template.execute(rowReader, new CsvItemHandler(processedRows));

        assertThat(result.getTotalCount()).isEqualTo(4);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(2);
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getRowErrors()).hasSize(3);
        assertThat(result.getRowErrors())
                .extracting(ImportRowError::getField)
                .containsExactly("name", "quantity", null);
        assertThat(result.getRowErrors())
                .extracting(ImportRowError::getRowNumber)
                .containsExactly(2, 2, 3);
        assertThat(result.getRowErrors())
                .extracting(ImportRowError::getRawRowSummary)
                .containsExactly("3:[, 0]", "3:[, 0]", "4:[boom, 1]");
        assertThat(processedRows)
                .extracting(Item::name)
                .containsExactly("apple", "banana");
    }

    private static List<CsvRow> readRows(CsvImportRowReader reader) throws Exception {
        List<CsvRow> rows = new ArrayList<>();
        try (reader) {
            for (CsvRow row : reader) {
                rows.add(row);
            }
        }
        return rows;
    }

    private record Item(int sourceRowNumber, String name, int quantity) {
    }

    private static final class PassthroughCsvRowHandler implements ImportRowHandler<CsvRow, CsvRow> {

        private final List<CsvRow> processedRows;

        private PassthroughCsvRowHandler(List<CsvRow> processedRows) {
            this.processedRows = processedRows;
        }

        @Override
        public boolean isBlankRow(CsvRow rawRow) {
            return rawRow == null || rawRow.isBlank();
        }

        @Override
        public CsvRow parse(CsvRow rawRow, ImportRowContext context) {
            return rawRow;
        }

        @Override
        public void process(CsvRow row, ImportRowContext context) {
            processedRows.add(row);
        }
    }

    private static final class CsvItemHandler implements ImportRowHandler<CsvRow, Item> {

        private final List<Item> processedRows;

        private CsvItemHandler(List<Item> processedRows) {
            this.processedRows = processedRows;
        }

        @Override
        public boolean isBlankRow(CsvRow rawRow) {
            return rawRow == null || rawRow.isBlank();
        }

        @Override
        public Item parse(CsvRow rawRow, ImportRowContext context) {
            String name = rawRow.get("name") == null ? "" : rawRow.get("name").trim();
            String quantityText = rawRow.get("quantity") == null ? "" : rawRow.get("quantity").trim();
            int quantity = quantityText.isBlank() ? 0 : Integer.parseInt(quantityText);
            return new Item(rawRow.getRowNumber(), name, quantity);
        }

        @Override
        public List<ImportRowError> validate(Item row, ImportRowContext context) {
            List<ImportRowError> errors = new ArrayList<>();
            if (row.name().isBlank()) {
                errors.add(ImportRowError.of("name", "NAME_REQUIRED", "name is required"));
            }
            if (row.quantity() <= 0) {
                errors.add(ImportRowError.of("quantity", "QUANTITY_POSITIVE", "quantity must be positive"));
            }
            return errors;
        }

        @Override
        public void process(Item row, ImportRowContext context) {
            if ("boom".equals(row.name())) {
                throw new IllegalStateException("boom item cannot be processed");
            }
            processedRows.add(row);
        }

        @Override
        public String summarizeRawRow(CsvRow rawRow) {
            return rawRow.getRowNumber() + ":" + rawRow.getValues();
        }
    }

    private static final class CloseTrackingReader extends StringReader {

        private boolean closed;

        private CloseTrackingReader(String source) {
            super(source);
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }

        private boolean isClosed() {
            return closed;
        }
    }
}
