package com.undernine.utils.biz.importtask;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * CSV 导入行读取器。
 * <p>
 * 该读取器只负责把 CSV 输入源解析为 {@link CsvRow}，业务校验、跳过统计、失败收集仍交给
 * {@link ImportTaskTemplate} 和 {@link ImportRowHandler}。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CsvImportRowReader implements ImportRowReader<CsvRow> {

    private static final char DEFAULT_DELIMITER = ',';
    private static final char DEFAULT_QUOTE = '"';
    private static final char UTF8_BOM = '\ufeff';
    private static final int DEFAULT_MAX_RECORD_CHARS = 1024 * 1024;

    private final Reader reader;
    private final boolean hasHeader;
    private final char delimiter;
    private final char quote;
    private final BlankRowPolicy blankRowPolicy;
    private final boolean strictQuotes;
    private final int maxRecordChars;

    private boolean iteratorCreated;
    private boolean closed;
    private boolean endOfFile;
    private int lineNumber = 1;
    private int pendingChar = -1;

    private CsvImportRowReader(Builder builder) {
        this.reader = builder.reader;
        this.hasHeader = builder.hasHeader;
        this.delimiter = builder.delimiter;
        this.quote = builder.quote;
        this.blankRowPolicy = builder.blankRowPolicy;
        this.strictQuotes = builder.strictQuotes;
        this.maxRecordChars = builder.maxRecordChars;
    }

    /**
     * 创建默认 CSV 读取器。
     *
     * @param reader 输入 Reader
     * @return CSV 读取器
     */
    public static CsvImportRowReader of(Reader reader) {
        return builder(reader).build();
    }

    /**
     * 创建 CSV 读取器构建器。
     *
     * @param reader 输入 Reader
     * @return 构建器
     */
    public static Builder builder(Reader reader) {
        return new Builder(reader);
    }

    @Override
    public Iterator<CsvRow> iterator() {
        if (iteratorCreated) {
            throw new IllegalStateException("CsvImportRowReader can be iterated only once");
        }
        iteratorCreated = true;
        return new CsvIterator();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            reader.close();
        }
    }

    private RawCsvRecord readNextRecord() throws IOException {
        if (endOfFile) {
            return null;
        }

        List<String> values = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean quoteClosed = false;
        boolean fieldStarted = false;
        boolean recordHasCharacter = false;
        int recordStartLine = lineNumber;
        int recordCharCount = 0;

        while (true) {
            int ch = readChar();
            if (ch == -1) {
                endOfFile = true;
                if (!recordHasCharacter && !fieldStarted && values.isEmpty()) {
                    return null;
                }
                if (inQuotes && strictQuotes) {
                    throw malformedRecord(recordStartLine, "quoted field is not closed");
                }
                values.add(field.toString());
                return new RawCsvRecord(recordStartLine, values);
            }
            if (recordStartLine == 1 && !recordHasCharacter && !fieldStarted && values.isEmpty()
                    && ch == UTF8_BOM) {
                continue;
            }

            recordHasCharacter = true;
            recordCharCount++;
            if (recordCharCount > maxRecordChars) {
                throw malformedRecord(recordStartLine, "record length exceeds " + maxRecordChars + " characters");
            }

            if (inQuotes) {
                if (ch == quote) {
                    inQuotes = false;
                    quoteClosed = true;
                } else {
                    field.append((char) ch);
                }
                fieldStarted = true;
                continue;
            }

            if (quoteClosed) {
                if (ch == quote) {
                    field.append(quote);
                    inQuotes = true;
                    quoteClosed = false;
                    fieldStarted = true;
                    continue;
                }
                if (ch == delimiter) {
                    values.add(field.toString());
                    field.setLength(0);
                    fieldStarted = false;
                    quoteClosed = false;
                    continue;
                }
                if (ch == '\n') {
                    values.add(field.toString());
                    return new RawCsvRecord(recordStartLine, values);
                }
                if (strictQuotes) {
                    if (Character.isWhitespace(ch)) {
                        continue;
                    }
                    throw malformedRecord(recordStartLine, "unexpected character after closing quote");
                }
                field.append((char) ch);
                quoteClosed = false;
                fieldStarted = true;
                continue;
            }

            if (!fieldStarted && ch == quote) {
                inQuotes = true;
                fieldStarted = true;
                continue;
            }
            if (ch == delimiter) {
                values.add(field.toString());
                field.setLength(0);
                fieldStarted = false;
                continue;
            }
            if (ch == '\n') {
                values.add(field.toString());
                return new RawCsvRecord(recordStartLine, values);
            }
            if (strictQuotes && ch == quote) {
                throw malformedRecord(recordStartLine, "quote must be the first character of a quoted field");
            }

            field.append((char) ch);
            fieldStarted = true;
        }
    }

    private ImportTaskException malformedRecord(int recordStartLine, String reason) {
        return new ImportTaskException("Malformed CSV record at line " + recordStartLine + ": " + reason);
    }

    private int readChar() throws IOException {
        int ch;
        if (pendingChar >= 0) {
            ch = pendingChar;
            pendingChar = -1;
        } else {
            ch = reader.read();
        }

        if (ch == '\r') {
            int next = reader.read();
            if (next != '\n' && next != -1) {
                pendingChar = next;
            }
            lineNumber++;
            return '\n';
        }
        if (ch == '\n') {
            lineNumber++;
            return '\n';
        }
        return ch;
    }

    private void ensureOpen() {
        if (closed) {
            throw new ImportTaskException("CSV import row reader is already closed");
        }
    }

    private static boolean isBlank(List<String> values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * CSV 空白行策略。
     */
    public enum BlankRowPolicy {

        /**
         * 保留空白行，由导入模板和行处理器决定是否跳过并计数。
         */
        KEEP,

        /**
         * 读取阶段直接跳过空白行，导入模板不会感知这些行。
         */
        SKIP
    }

    /**
     * CSV 导入行读取器构建器。
     */
    public static final class Builder {

        private final Reader reader;
        private boolean hasHeader;
        private char delimiter = DEFAULT_DELIMITER;
        private char quote = DEFAULT_QUOTE;
        private BlankRowPolicy blankRowPolicy = BlankRowPolicy.KEEP;
        private boolean strictQuotes = true;
        private int maxRecordChars = DEFAULT_MAX_RECORD_CHARS;

        private Builder(Reader reader) {
            this.reader = Objects.requireNonNull(reader, "reader must not be null");
        }

        /**
         * 设置第一条 CSV 记录是否为表头。
         *
         * @param hasHeader true 表示第一条记录为表头
         * @return 当前构建器
         */
        public Builder hasHeader(boolean hasHeader) {
            this.hasHeader = hasHeader;
            return this;
        }

        /**
         * 设置 CSV 分隔符。
         *
         * @param delimiter 分隔符
         * @return 当前构建器
         */
        public Builder delimiter(char delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * 设置 CSV 引号字符。
         *
         * @param quote 引号字符
         * @return 当前构建器
         */
        public Builder quote(char quote) {
            this.quote = quote;
            return this;
        }

        /**
         * 设置空白行策略。
         *
         * @param blankRowPolicy 空白行策略
         * @return 当前构建器
         */
        public Builder blankRowPolicy(BlankRowPolicy blankRowPolicy) {
            this.blankRowPolicy = Objects.requireNonNull(blankRowPolicy, "blankRowPolicy must not be null");
            return this;
        }

        /**
         * 设置是否严格校验 CSV 引号语法。
         *
         * @param strictQuotes true 表示遇到未闭合引号或闭合引号后的非法字符时立即失败
         * @return 当前构建器
         */
        public Builder strictQuotes(boolean strictQuotes) {
            this.strictQuotes = strictQuotes;
            return this;
        }

        /**
         * 设置单条 CSV 记录允许的最大字符数，用于防止异常大行耗尽内存。
         *
         * @param maxRecordChars 最大字符数
         * @return 当前构建器
         */
        public Builder maxRecordChars(int maxRecordChars) {
            if (maxRecordChars <= 0) {
                throw new IllegalArgumentException("maxRecordChars must be greater than 0");
            }
            this.maxRecordChars = maxRecordChars;
            return this;
        }

        /**
         * 构建 CSV 读取器。
         *
         * @return CSV 读取器
         */
        public CsvImportRowReader build() {
            validateControlCharacter(delimiter, "delimiter");
            validateControlCharacter(quote, "quote");
            if (delimiter == quote) {
                throw new IllegalArgumentException("delimiter must not be same as quote");
            }
            return new CsvImportRowReader(this);
        }

        private static void validateControlCharacter(char value, String name) {
            if (value == '\r' || value == '\n') {
                throw new IllegalArgumentException(name + " must not be a line separator");
            }
        }
    }

    private final class CsvIterator implements Iterator<CsvRow> {

        private boolean headerLoaded;
        private List<String> headers = Collections.emptyList();
        private boolean nextLoaded;
        private CsvRow next;

        @Override
        public boolean hasNext() {
            loadNextIfNecessary();
            return next != null;
        }

        @Override
        public CsvRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more CSV import rows");
            }
            CsvRow current = next;
            next = null;
            nextLoaded = false;
            return current;
        }

        private void loadNextIfNecessary() {
            if (nextLoaded) {
                return;
            }
            ensureOpen();
            try {
                loadHeaderIfNecessary();
                RawCsvRecord record = readNextRecord();
                while (record != null && blankRowPolicy == BlankRowPolicy.SKIP && isBlank(record.values())) {
                    record = readNextRecord();
                }
                next = record == null ? null : new CsvRow(record.rowNumber(), headers, record.values());
                nextLoaded = true;
            } catch (ImportTaskException ex) {
                throw ex;
            } catch (IOException ex) {
                throw new ImportTaskException("Failed to read CSV import row", ex);
            }
        }

        private void loadHeaderIfNecessary() throws IOException {
            if (headerLoaded) {
                return;
            }
            if (hasHeader) {
                RawCsvRecord headerRecord = readNextRecord();
                headers = headerRecord == null ? Collections.emptyList() : headerRecord.values();
            }
            headerLoaded = true;
        }
    }

    private record RawCsvRecord(int rowNumber, List<String> values) {
    }
}
