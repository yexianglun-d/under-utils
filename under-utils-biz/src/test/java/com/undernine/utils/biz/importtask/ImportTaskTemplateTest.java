package com.undernine.utils.biz.importtask;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportTaskTemplateTest {

    @Test
    void execute_successImport() {
        List<Item> processedRows = new ArrayList<>();
        ImportTaskTemplate template = ImportTaskTemplate.create();

        ImportResult result = template.execute(List.of("apple,10", "banana,20"), new CsvItemHandler(processedRows));

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isZero();
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getRowErrors()).isEmpty();
        assertThat(result.isAllSuccess()).isTrue();
        assertThat(processedRows)
                .extracting(Item::name)
                .containsExactly("apple", "banana");
    }

    @Test
    void execute_validationFailureCollectsMultipleErrors() {
        List<Item> processedRows = new ArrayList<>();
        ImportTaskTemplate template = ImportTaskTemplate.create();

        ImportResult result = template.execute(List.of(",0", "apple,10"), new CsvItemHandler(processedRows));

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.isAllSuccess()).isFalse();
        assertThat(result.getRowErrors()).hasSize(2);
        assertThat(result.getRowErrors())
                .extracting(ImportRowError::getField)
                .containsExactly("name", "quantity");
        assertThat(result.getRowErrors())
                .extracting(ImportRowError::getRowNumber)
                .containsExactly(1, 1);
        assertThat(result.getRowErrors())
                .extracting(ImportRowError::getRawRowSummary)
                .containsExactly(",0", ",0");
        assertThat(processedRows)
                .extracting(Item::name)
                .containsExactly("apple");
    }

    @Test
    void execute_failFastStopsAfterFirstFailedRow() {
        ImportTaskTemplate template = ImportTaskTemplate.create(ImportOptions.builder()
                .failFast(true)
                .build());
        List<Item> processedRows = new ArrayList<>();

        ImportResult result = template.execute(List.of(",0", "apple,10"), new CsvItemHandler(processedRows));

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getRowErrors()).hasSize(2);
        assertThat(processedRows).isEmpty();
    }

    @Test
    void execute_maxErrorsStopsAfterErrorLimitIsReached() {
        ImportTaskTemplate template = ImportTaskTemplate.create(ImportOptions.builder()
                .maxErrors(2)
                .build());

        ImportResult result = template.execute(List.of(",1", ",2", ",3"), new CsvItemHandler(new ArrayList<>()));

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailureCount()).isEqualTo(2);
        assertThat(result.getRowErrors()).hasSize(2);
        assertThat(result.getRowErrors())
                .extracting(ImportRowError::getRowNumber)
                .containsExactly(1, 2);
    }

    @Test
    void execute_skipBlankRows() {
        List<Item> processedRows = new ArrayList<>();
        ImportTaskTemplate template = ImportTaskTemplate.create();

        ImportResult result = template.execute(List.of("", "apple,10", "   ", "banana,20"),
                new CsvItemHandler(processedRows));

        assertThat(result.getTotalCount()).isEqualTo(4);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getSkippedCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isZero();
        assertThat(result.isAllSuccess()).isTrue();
        assertThat(processedRows)
                .extracting(Item::name)
                .containsExactly("apple", "banana");
    }

    @Test
    void execute_processExceptionConvertedToRowError() {
        List<Item> processedRows = new ArrayList<>();
        ImportTaskTemplate template = ImportTaskTemplate.create();

        ImportResult result = template.execute(List.of("apple,10", "boom,1", "banana,20"),
                new CsvItemHandler(processedRows));

        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getRowErrors()).singleElement().satisfies(error -> {
            assertThat(error.getRowNumber()).isEqualTo(2);
            assertThat(error.getErrorCode()).isEqualTo(ImportErrorCodes.PROCESS_ERROR);
            assertThat(error.getMessage()).isEqualTo("boom item cannot be processed");
            assertThat(error.getRawRowSummary()).isEqualTo("boom,1");
        });
        assertThat(processedRows)
                .extracting(Item::name)
                .containsExactly("apple", "banana");
    }

    @Test
    void execute_rowValidationExceptionConvertedToRowErrors() {
        ImportTaskTemplate template = ImportTaskTemplate.create();
        ImportRowHandler<String, String> handler = new ImportRowHandler<>() {
            @Override
            public String parse(String rawRow, ImportRowContext context) {
                throw new RowValidationException(ImportRowError.of("name", "NAME_INVALID", "name is invalid"));
            }

            @Override
            public void process(String row, ImportRowContext context) {
            }
        };

        ImportResult result = template.execute(List.of("bad"), handler);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getRowErrors()).singleElement().satisfies(error -> {
            assertThat(error.getRowNumber()).isEqualTo(1);
            assertThat(error.getField()).isEqualTo("name");
            assertThat(error.getErrorCode()).isEqualTo("NAME_INVALID");
            assertThat(error.getRawRowSummary()).isEqualTo("bad");
        });
    }

    @Test
    void execute_importTaskExceptionPropagatesAsTaskFailure() {
        ImportTaskTemplate template = ImportTaskTemplate.create();
        ImportRowHandler<String, String> handler = new ImportRowHandler<>() {
            @Override
            public String parse(String rawRow, ImportRowContext context) {
                throw new ImportTaskException("import configuration missing");
            }

            @Override
            public void process(String row, ImportRowContext context) {
            }
        };

        assertThatThrownBy(() -> template.execute(List.of("row"), handler))
                .isInstanceOf(ImportTaskException.class)
                .hasMessage("import configuration missing");
    }

    @Test
    void execute_reportsProgressWithoutChangingImportResult() {
        List<ImportProgress> progresses = new ArrayList<>();
        ImportTaskTemplate template = ImportTaskTemplate.create(ImportOptions.builder()
                .progressListener(progresses::add)
                .build());

        ImportResult result = template.execute(List.of("apple,10", ",0"), new CsvItemHandler(new ArrayList<>()));

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(progresses).isNotEmpty();
        assertThat(progresses.get(0).getStatus()).isEqualTo(ImportTaskStatus.RUNNING);
        assertThat(progresses.get(progresses.size() - 1).getStatus()).isEqualTo(ImportTaskStatus.COMPLETED);
        assertThat(progresses.get(progresses.size() - 1).getErrorCount()).isEqualTo(2);
    }

    @Test
    void asyncTemplateStoresResultAndProgress() {
        Executor directExecutor = Runnable::run;
        try (AsyncImportTaskTemplate template = new AsyncImportTaskTemplate(directExecutor)) {

            String taskId = template.submit("task-1", List.of("apple,10", ",0"), new CsvItemHandler(new ArrayList<>()));

            assertThat(taskId).isEqualTo("task-1");
            assertThat(template.findProgress("task-1")).hasValueSatisfying(progress -> {
                assertThat(progress.getTaskId()).isEqualTo("task-1");
                assertThat(progress.getStatus()).isEqualTo(ImportTaskStatus.COMPLETED);
                assertThat(progress.getTotalCount()).isEqualTo(2);
                assertThat(progress.getErrorCount()).isEqualTo(2);
            });
            assertThat(template.findResult("task-1")).hasValueSatisfying(result -> {
                assertThat(result.getSuccessCount()).isEqualTo(1);
                assertThat(result.getFailureCount()).isEqualTo(1);
            });
            assertThat(template.findFailure("task-1")).isEmpty();
        }
    }

    @Test
    void asyncTemplateStoresTaskFailure() {
        LogCapture logCapture = captureLogs(AsyncImportTaskTemplate.class);
        try (logCapture; AsyncImportTaskTemplate template = new AsyncImportTaskTemplate(Runnable::run)) {
            ImportRowHandler<String, String> handler = failingTaskHandler("reader configuration missing");

            template.submit("failed-task", List.of("row"), handler);

            assertThat(template.findProgress("failed-task")).hasValueSatisfying(progress -> {
                assertThat(progress.getStatus()).isEqualTo(ImportTaskStatus.FAILED);
                assertThat(progress.getFailureMessage()).isEqualTo("reader configuration missing");
            });
            assertThat(template.findResult("failed-task")).isEmpty();
            assertThat(template.findFailure("failed-task")).hasValueSatisfying(error ->
                    assertThat(error).isInstanceOf(ImportTaskException.class));
            assertThat(logCapture.events())
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        assertThat(event.getFormattedMessage())
                                .isEqualTo("Async import task failed: failed-task");
                        assertThat(event.getThrowableProxy().getMessage())
                                .isEqualTo("reader configuration missing");
                    });
        }
    }

    @Test
    void asyncTemplateExpiresCompletedTaskState() throws Exception {
        try (AsyncImportTaskTemplate template = new AsyncImportTaskTemplate(
                ImportOptions.defaults(), Runnable::run, Duration.ofMillis(5))) {

            template.submit("expire-task", List.of("apple,10"), new CsvItemHandler(new ArrayList<>()));
            assertThat(template.findProgress("expire-task")).isPresent();

            Thread.sleep(20L);

            assertThat(template.findProgress("expire-task")).isEmpty();
            assertThat(template.findResult("expire-task")).isEmpty();
            assertThat(template.findFailure("expire-task")).isEmpty();
        }
    }

    @Test
    void asyncTemplateActivelyRemovesExpiredTaskState() throws Exception {
        try (AsyncImportTaskTemplate template = new AsyncImportTaskTemplate(
                ImportOptions.defaults(), Runnable::run, Duration.ofMillis(5), Duration.ofMillis(10))) {

            template.submit("active-expire-task", List.of("apple,10"), new CsvItemHandler(new ArrayList<>()));
            assertThat(template.size()).isEqualTo(1);

            waitUntil(() -> template.size() == 0);

            assertThat(template.findProgress("active-expire-task")).isEmpty();
            assertThat(template.size()).isZero();
        }
    }

    @Test
    void asyncTemplateMarksTaskFailedWhenExecutorRejects() {
        try (AsyncImportTaskTemplate template = new AsyncImportTaskTemplate(command -> {
            throw new RejectedExecutionException("queue full");
        })) {

            assertThatThrownBy(() -> template.submit("rejected-task", List.of("apple,10"),
                    new CsvItemHandler(new ArrayList<>())))
                    .isInstanceOf(RejectedExecutionException.class)
                    .hasMessage("queue full");

            assertThat(template.findProgress("rejected-task")).hasValueSatisfying(progress -> {
                assertThat(progress.getStatus()).isEqualTo(ImportTaskStatus.FAILED);
                assertThat(progress.getFailureMessage()).isEqualTo("queue full");
            });
        }
    }

    @Test
    void errorExporterWritesCsvWithEscaping() {
        String csv = ImportErrorExporter.toCsv(List.of(
                new ImportRowError(2, "name", "NAME_INVALID", "contains, comma", "Alice,\"A\"")
        ));

        assertThat(csv).isEqualTo("rowNumber,field,errorCode,message,rawRowSummary\n"
                + "2,name,NAME_INVALID,\"contains, comma\",\"Alice,\"\"A\"\"\"\n");
    }

    @Test
    void progressListenerFailureDoesNotBreakImport() {
        AtomicInteger callbackCount = new AtomicInteger();
        ImportTaskTemplate template = ImportTaskTemplate.create(ImportOptions.builder()
                .progressListener(progress -> {
                    callbackCount.incrementAndGet();
                    throw new IllegalStateException("metrics down");
                })
                .build());

        ImportResult result;
        try (LogCapture logCapture = captureLogs(ImportTaskTemplate.class)) {
            result = template.execute(List.of("apple,10"), new CsvItemHandler(new ArrayList<>()));
            assertThat(logCapture.events())
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        assertThat(event.getFormattedMessage()).isEqualTo("Import progress listener failed");
                        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("metrics down");
                    });
        }

        assertThat(result.isAllSuccess()).isTrue();
        assertThat(callbackCount).hasPositiveValue();
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1_000L;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
    }

    private static ImportRowHandler<String, String> failingTaskHandler(String message) {
        return new ImportRowHandler<>() {
            @Override
            public String parse(String rawRow, ImportRowContext context) {
                throw new ImportTaskException(message);
            }

            @Override
            public void process(String row, ImportRowContext context) {
            }
        };
    }

    private static LogCapture captureLogs(Class<?> loggerClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        return new LogCapture(logger);
    }

    private record Item(String name, int quantity) {
    }

    private static final class CsvItemHandler implements ImportRowHandler<String, Item> {

        private final List<Item> processedRows;

        private CsvItemHandler(List<Item> processedRows) {
            this.processedRows = processedRows;
        }

        @Override
        public boolean isBlankRow(String rawRow) {
            return rawRow == null || rawRow.isBlank();
        }

        @Override
        public Item parse(String rawRow, ImportRowContext context) {
            String[] parts = rawRow.split(",", -1);
            return new Item(parts[0].trim(), Integer.parseInt(parts[1].trim()));
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
    }

    private static final class LogCapture implements AutoCloseable {

        private final Logger logger;
        private final boolean additive;
        private final Level level;
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

        private LogCapture(Logger logger) {
            this.logger = logger;
            this.additive = logger.isAdditive();
            this.level = logger.getLevel();
            appender.start();
            logger.addAppender(appender);
            logger.setAdditive(false);
        }

        private List<ILoggingEvent> events() {
            return appender.list;
        }

        @Override
        public void close() {
            logger.detachAppender(appender);
            logger.setAdditive(additive);
            logger.setLevel(level);
            appender.stop();
        }
    }
}
