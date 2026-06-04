# Under-Utils Biz

可复用业务流程模板模块。

本模块用于放置跨应用反复出现、但又比 `core` 更接近业务流程形态的能力。它不应该包含订单、支付、会员、营销等产品域专属模型。

## 依赖

```xml
<dependency>
    <groupId>io.github.yexianglun-d</groupId>
    <artifactId>under-utils-biz</artifactId>
    <version>1.0.3</version>
</dependency>
```

## 导入任务模板

`ImportTaskTemplate` 会让每一行依次经过三个阶段：

1. `parse`
2. `validate`
3. `process`

模板负责收集行级错误并返回导入统计。需要查询进度时，可以通过 `ImportOptions.progressListener` 接收快照。

```java
ImportTaskTemplate template = ImportTaskTemplate.create(
        ImportOptions.builder()
                .skipBlankRows(true)
                .maxErrors(100)
                .progressListener(progress -> progressService.update(progress))
                .build()
);

ImportResult result = template.execute(rows, new ImportRowHandler<String, UserImportRow>() {

    @Override
    public boolean isBlankRow(String rawRow) {
        return rawRow == null || rawRow.isBlank();
    }

    @Override
    public UserImportRow parse(String rawRow, ImportRowContext context) {
        String[] parts = rawRow.split(",", -1);
        return new UserImportRow(parts[0].trim(), parts[1].trim());
    }

    @Override
    public List<ImportRowError> validate(UserImportRow row, ImportRowContext context) {
        List<ImportRowError> errors = new ArrayList<>();
        if (row.username().isBlank()) {
            errors.add(ImportRowError.of("username", "USERNAME_REQUIRED", "username is required"));
        }
        if (row.phone().isBlank()) {
            errors.add(ImportRowError.of("phone", "PHONE_REQUIRED", "phone is required"));
        }
        return errors;
    }

    @Override
    public void process(UserImportRow row, ImportRowContext context) {
        userService.importUser(row);
    }
});
```

## CSV Reader

`CsvImportRowReader` 将 CSV 输入解析为 `CsvRow`，并在模板执行结束后关闭 reader：

```java
try (Reader reader = Files.newBufferedReader(path)) {
    ImportResult result = ImportTaskTemplate.create()
            .execute(CsvImportRowReader.builder(reader)
                    .hasHeader(true)
                    .build(), handler);
}
```

CSV 解析保持小而可预测。Excel、大文件流式导入等场景，建议业务项目自行解析文件，再把行数据交给 `ImportTaskTemplate`。

默认 CSV reader 会：

- 自动忽略文件开头的 UTF-8 BOM。
- 严格校验引号语法，未闭合引号或闭合引号后的非法字符会抛出 `ImportTaskException`。
- 限制单条记录最大字符数，默认 1 MiB，避免异常大行耗尽内存。

需要兼容历史宽松引号文件时，可以显式关闭严格模式；处理可信内部文件且单行较大时，可以调高记录长度上限：

```java
CsvImportRowReader reader = CsvImportRowReader.builder(source)
        .hasHeader(true)
        .strictQuotes(false)
        .maxRecordChars(2 * 1024 * 1024)
        .build();
```

## 异步导入

`AsyncImportTaskTemplate` 使用业务传入的 `Executor` 执行后台导入，并在当前 JVM 内保存任务进度、完成结果或任务级失败。

```java
AsyncImportTaskTemplate asyncTemplate = new AsyncImportTaskTemplate(executor);

String taskId = asyncTemplate.submit("user-import-20260523", rows, handler);

ImportProgress progress = asyncTemplate.findProgress(taskId)
        .orElseThrow();

ImportResult result = asyncTemplate.findResult(taskId)
        .orElse(null);
```

默认状态存储是内存 Map，适合单实例后台任务或示例工程。完成或失败的任务默认在当前 JVM 内保留 24 小时，
并由每实例后台清理线程主动移除；如需调整，可使用带 `Duration taskRetention` 和 `Duration cleanupInterval`
的构造器。`AsyncImportTaskTemplate` 实现了 `AutoCloseable`，手动创建时可在不再使用后调用 `close()`；
作为 Spring Bean 使用时会随应用上下文关闭。生产环境如果需要跨实例查询、重启恢复或任务审计，应通过
`ImportProgressListener` 将进度写入数据库、Redis 或消息系统。

## 错误导出

行级错误可以导出为 CSV，供接口直接返回或写入对象存储：

```java
String csv = ImportErrorExporter.toCsv(result.getRowErrors());
```

CSV 字段为：

- `rowNumber`
- `field`
- `errorCode`
- `message`
- `rawRowSummary`

## 失败语义

- `RowValidationException` 会转换为行级错误。
- parse、validate、process 阶段的其他异常会按阶段转换为行级错误。
- `ImportTaskException` 视为任务级失败，直接向外传播。
- `failFast` 会在首个失败行后停止。
- `maxErrors` 会在收集错误数达到上限后停止。

## 结果字段

`ImportResult` 包含：

- 总行数
- 成功行数
- 失败行数
- 跳过行数
- 行级错误列表

调用方可以根据结果决定提交、回滚、展示预览，或要求用户修正后重试。
