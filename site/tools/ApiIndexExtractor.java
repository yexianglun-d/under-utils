import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiIndexExtractor {

    private static final Map<String, String> ARTIFACT_MODULES = new LinkedHashMap<>();
    private static final Pattern INLINE_TAG = Pattern.compile("\\{@(?:code|link|linkplain|literal)\\s+([^}]+)}");
    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    static {
        ARTIFACT_MODULES.put("under-utils-core", "core");
        ARTIFACT_MODULES.put("under-utils-spring", "spring");
        ARTIFACT_MODULES.put("under-utils-redis", "redis");
        ARTIFACT_MODULES.put("under-utils-http", "http");
        ARTIFACT_MODULES.put("under-utils-ai", "ai");
        ARTIFACT_MODULES.put("under-utils-security", "security");
        ARTIFACT_MODULES.put("under-utils-mybatis", "mybatis");
        ARTIFACT_MODULES.put("under-utils-biz", "biz");
        ARTIFACT_MODULES.put("under-utils-ai-starter", "ai-starter");
        ARTIFACT_MODULES.put("under-utils-security-starter", "security-starter");
        ARTIFACT_MODULES.put("under-utils-mybatis-starter", "mybatis-starter");
        ARTIFACT_MODULES.put("under-utils-spring-starter", "spring-starter");
        ARTIFACT_MODULES.put("under-utils-redis-starter", "redis-starter");
        ARTIFACT_MODULES.put("under-utils-starter", "starter");
    }

    public static void main(String[] args) throws IOException {
        Arguments arguments = Arguments.parse(args);
        Path root = arguments.root().toAbsolutePath().normalize();
        List<SourceUnit> sources = findSources(root);
        List<ApiItem> items = parseSources(root, sources);
        writeJson(arguments.output(), root, sources, items);
    }

    private static List<SourceUnit> findSources(Path root) throws IOException {
        List<SourceUnit> sources = new ArrayList<>();
        for (Map.Entry<String, String> entry : ARTIFACT_MODULES.entrySet()) {
            Path sourceRoot = root.resolve(entry.getKey()).resolve("src/main/java");
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .sorted()
                    .forEach(path -> sources.add(new SourceUnit(path, entry.getKey(), entry.getValue())));
            }
        }
        return sources;
    }

    private static List<ApiItem> parseSources(Path root, List<SourceUnit> sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler is required to generate API index.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromPaths(
                sources.stream().map(SourceUnit::path).toList()
            );
            List<String> options = List.of("-proc:none", "-encoding", "UTF-8", "--release", "21");
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostics, options, null, fileObjects);
            DocTrees docTrees = DocTrees.instance(task);
            Iterable<? extends CompilationUnitTree> units = task.parse();

            Map<Path, SourceUnit> sourceByPath = sources.stream()
                .collect(Collectors.toMap(unit -> unit.path().toAbsolutePath().normalize(), unit -> unit));
            List<ApiItem> items = new ArrayList<>();
            for (CompilationUnitTree unit : units) {
                SourceUnit sourceUnit = sourceByPath.get(Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize());
                if (sourceUnit == null) {
                    continue;
                }
                new ApiVisitor(root, sourceUnit, unit, docTrees, items).scan(unit, null);
            }

            List<Diagnostic<? extends JavaFileObject>> errors = diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .toList();
            if (!errors.isEmpty()) {
                String message = errors.stream().limit(8)
                    .map(diagnostic -> diagnostic.getSource() + ":" + diagnostic.getLineNumber() + " " + diagnostic.getMessage(Locale.ROOT))
                    .collect(Collectors.joining("\n"));
                throw new IllegalStateException("Failed to parse Java sources:\n" + message);
            }

            items.sort(Comparator
                .comparing(ApiItem::module)
                .thenComparing(ApiItem::packageName)
                .thenComparing(ApiItem::name));
            return items;
        }
    }

    private static void writeJson(Path output, Path root, List<SourceUnit> sources, List<ApiItem> items) throws IOException {
        Files.createDirectories(output.getParent());
        int memberCount = items.stream().mapToInt(item -> item.members().size()).sum();
        Map<String, Long> countsByArtifact = items.stream()
            .collect(Collectors.groupingBy(ApiItem::artifact, TreeMap::new, Collectors.counting()));

        StringBuilder json = new StringBuilder(1024 * 256);
        json.append("{\n");
        json.append("  \"meta\": {\n");
        json.append("    \"generatedBy\": \"JDK Compiler Tree API\",\n");
        json.append("    \"source\": \"src/main/java public and protected API\",\n");
        json.append("    \"sourceFileCount\": ").append(sources.size()).append(",\n");
        json.append("    \"artifactCount\": ").append(countsByArtifact.size()).append(",\n");
        json.append("    \"typeCount\": ").append(items.size()).append(",\n");
        json.append("    \"memberCount\": ").append(memberCount).append(",\n");
        json.append("    \"artifacts\": [\n");
        int artifactIndex = 0;
        for (Map.Entry<String, Long> entry : countsByArtifact.entrySet()) {
            if (artifactIndex++ > 0) {
                json.append(",\n");
            }
            json.append("      {\"artifact\": ").append(quote(entry.getKey()))
                .append(", \"module\": ").append(quote(ARTIFACT_MODULES.get(entry.getKey())))
                .append(", \"typeCount\": ").append(entry.getValue()).append("}");
        }
        json.append("\n    ]\n");
        json.append("  },\n");
        json.append("  \"items\": [\n");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                json.append(",\n");
            }
            writeItem(json, root, items.get(i));
        }
        json.append("\n  ]\n");
        json.append("}\n");
        Files.writeString(output, json.toString(), StandardCharsets.UTF_8);
    }

    private static void writeItem(StringBuilder json, Path root, ApiItem item) {
        json.append("    {\n");
        property(json, "name", item.name(), 6, true);
        property(json, "qualifiedName", item.qualifiedName(), 6, true);
        property(json, "module", item.module(), 6, true);
        property(json, "artifact", item.artifact(), 6, true);
        property(json, "packageName", item.packageName(), 6, true);
        property(json, "type", item.type(), 6, true);
        property(json, "status", item.status(), 6, true);
        property(json, "summary", item.summary(), 6, true);
        property(json, "docsPath", "/docs/modules/" + item.module() + "/", 6, true);
        property(json, "sourcePath", root.relativize(item.sourcePath()).toString().replace('\\', '/'), 6, true);
        property(json, "since", item.since(), 6, true);
        property(json, "deprecatedMessage", item.deprecatedMessage(), 6, true);
        json.append("      \"members\": [");
        if (!item.members().isEmpty()) {
            json.append("\n");
            for (int i = 0; i < item.members().size(); i++) {
                if (i > 0) {
                    json.append(",\n");
                }
                ApiMember member = item.members().get(i);
                json.append("        {\n");
                property(json, "name", member.name(), 10, true);
                property(json, "kind", member.kind(), 10, true);
                property(json, "signature", member.signature(), 10, true);
                property(json, "status", member.status(), 10, true);
                property(json, "summary", member.summary(), 10, false);
                json.append("        }");
            }
            json.append("\n      ");
        }
        json.append("]\n");
        json.append("    }");
    }

    private static void property(StringBuilder json, String key, String value, int indent, boolean comma) {
        json.append(" ".repeat(indent)).append(quote(key)).append(": ").append(quote(value == null ? "" : value));
        if (comma) {
            json.append(",");
        }
        json.append("\n");
    }

    private static String quote(String value) {
        StringBuilder quoted = new StringBuilder(value.length() + 16);
        quoted.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> quoted.append("\\\\");
                case '"' -> quoted.append("\\\"");
                case '\b' -> quoted.append("\\b");
                case '\f' -> quoted.append("\\f");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) ch));
                    } else {
                        quoted.append(ch);
                    }
                }
            }
        }
        quoted.append('"');
        return quoted.toString();
    }

    private record Arguments(Path root, Path output) {

        static Arguments parse(String[] args) {
            Path root = null;
            Path output = null;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--root" -> root = Path.of(args[++i]);
                    case "--output" -> output = Path.of(args[++i]);
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
            if (root == null || output == null) {
                throw new IllegalArgumentException("Usage: ApiIndexExtractor --root <repo> --output <json>");
            }
            return new Arguments(root, output);
        }
    }

    private record SourceUnit(Path path, String artifact, String module) {
    }

    private record ApiItem(
        String name,
        String qualifiedName,
        String module,
        String artifact,
        String packageName,
        String type,
        String status,
        String summary,
        Path sourcePath,
        String since,
        String deprecatedMessage,
        List<ApiMember> members
    ) {
    }

    private record ApiMember(String name, String kind, String signature, String status, String summary) {
    }

    private static final class TypeContext {

        private final String simpleName;
        private final boolean interfaceLike;
        private final boolean enumLike;

        private TypeContext(String simpleName, boolean interfaceLike, boolean enumLike) {
            this.simpleName = simpleName;
            this.interfaceLike = interfaceLike;
            this.enumLike = enumLike;
        }
    }

    private static final class ApiVisitor extends TreePathScanner<Void, Void> {

        private final Path root;
        private final SourceUnit source;
        private final CompilationUnitTree unit;
        private final DocTrees docTrees;
        private final List<ApiItem> items;
        private final Deque<TypeContext> stack = new ArrayDeque<>();

        private ApiVisitor(Path root, SourceUnit source, CompilationUnitTree unit, DocTrees docTrees, List<ApiItem> items) {
            this.root = root;
            this.source = source;
            this.unit = unit;
            this.docTrees = docTrees;
            this.items = items;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            TreePath path = getCurrentPath();
            boolean topLevel = stack.isEmpty();
            boolean interfaceLike = isInterfaceLike(node);
            boolean enumLike = node.getKind() == Tree.Kind.ENUM;

            if (topLevel && node.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                items.add(toItem(node, path));
            }

            stack.push(new TypeContext(node.getSimpleName().toString(), interfaceLike, enumLike));
            Void result = super.visitClass(node, unused);
            stack.pop();
            return result;
        }

        private ApiItem toItem(ClassTree node, TreePath path) {
            String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
            String name = node.getSimpleName().toString();
            DocCommentTree comment = docTrees.getDocCommentTree(path);
            String summary = summary(comment);
            List<ApiMember> members = collectMembers(node, path);
            return new ApiItem(
                name,
                packageName.isBlank() ? name : packageName + "." + name,
                source.module(),
                source.artifact(),
                packageName,
                typeOf(node),
                status(node.getModifiers(), comment),
                summary.isBlank() ? fallbackSummary(name, typeOf(node)) : summary,
                source.path().toAbsolutePath().normalize(),
                since(comment),
                deprecatedMessage(comment),
                members
            );
        }

        private List<ApiMember> collectMembers(ClassTree node, TreePath typePath) {
            boolean parentInterfaceLike = isInterfaceLike(node);
            boolean parentEnumLike = node.getKind() == Tree.Kind.ENUM;
            String typeName = node.getSimpleName().toString();
            List<ApiMember> members = new ArrayList<>();

            for (Tree member : node.getMembers()) {
                TreePath memberPath = new TreePath(typePath, member);
                if (member instanceof MethodTree method && isVisibleMember(method.getModifiers(), parentInterfaceLike, false)) {
                    DocCommentTree comment = docTrees.getDocCommentTree(memberPath);
                    members.add(new ApiMember(
                        methodName(method, typeName),
                        method.getName().contentEquals("<init>") ? "constructor" : "method",
                        methodSignature(method, typeName),
                        status(method.getModifiers(), comment),
                        summary(comment)
                    ));
                } else if (member instanceof VariableTree variable && isVisibleMember(variable.getModifiers(), parentInterfaceLike, parentEnumLike)) {
                    DocCommentTree comment = docTrees.getDocCommentTree(memberPath);
                    members.add(new ApiMember(
                        variable.getName().toString(),
                        "field",
                        fieldSignature(variable),
                        status(variable.getModifiers(), comment),
                        summary(comment)
                    ));
                } else if (member instanceof ClassTree nested && isVisibleMember(nested.getModifiers(), parentInterfaceLike, false)) {
                    DocCommentTree comment = docTrees.getDocCommentTree(memberPath);
                    members.add(new ApiMember(
                        nested.getSimpleName().toString(),
                        "nested-type",
                        nestedTypeSignature(nested),
                        status(nested.getModifiers(), comment),
                        summary(comment)
                    ));
                }
            }

            members.sort(Comparator
                .comparing(ApiMember::kind)
                .thenComparing(ApiMember::name)
                .thenComparing(ApiMember::signature));
            return members;
        }

        private static boolean isVisibleMember(ModifiersTree modifiers, boolean parentInterfaceLike, boolean parentEnumLike) {
            Set<Modifier> flags = modifiers.getFlags();
            if (flags.contains(Modifier.PRIVATE)) {
                return false;
            }
            if (parentInterfaceLike || parentEnumLike) {
                return true;
            }
            return flags.contains(Modifier.PUBLIC) || flags.contains(Modifier.PROTECTED);
        }

        private static boolean isInterfaceLike(ClassTree node) {
            return node.getKind() == Tree.Kind.INTERFACE || node.getKind() == Tree.Kind.ANNOTATION_TYPE;
        }

        private static String typeOf(ClassTree node) {
            return switch (node.getKind()) {
                case ANNOTATION_TYPE -> "annotation";
                case ENUM -> "enum";
                case INTERFACE -> "interface";
                case RECORD -> "record";
                default -> "class";
            };
        }

        private static String status(ModifiersTree modifiers, DocCommentTree comment) {
            boolean annotatedDeprecated = modifiers.getAnnotations().stream()
                .map(Object::toString)
                .anyMatch(annotation -> annotation.equals("@Deprecated") || annotation.startsWith("@Deprecated("));
            return annotatedDeprecated || hasTag(comment, "DEPRECATED") ? "deprecated" : "stable";
        }

        private static String deprecatedMessage(DocCommentTree comment) {
            if (comment == null) {
                return "";
            }
            return comment.getBlockTags().stream()
                .filter(tag -> tag.getKind().name().equals("DEPRECATED"))
                .map(Object::toString)
                .map(ApiVisitor::cleanDocText)
                .findFirst()
                .orElse("");
        }

        private static String since(DocCommentTree comment) {
            if (comment == null) {
                return "";
            }
            return comment.getBlockTags().stream()
                .filter(tag -> tag instanceof SinceTree)
                .map(tag -> cleanDocText(((SinceTree) tag).getBody().toString()))
                .findFirst()
                .orElse("");
        }

        private static boolean hasTag(DocCommentTree comment, String kind) {
            return comment != null && comment.getBlockTags().stream().anyMatch(tag -> tag.getKind().name().equals(kind));
        }

        private static String summary(DocCommentTree comment) {
            if (comment == null || comment.getFirstSentence().isEmpty()) {
                return "";
            }
            return cleanDocText(comment.getFirstSentence().stream().map(Objects::toString).collect(Collectors.joining(" ")));
        }

        private static String cleanDocText(String text) {
            return INLINE_TAG.matcher(decodeUnicodeEscapes(text))
                .replaceAll("$1")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("@deprecated", "")
                .replaceAll("\\s+", " ")
                .trim();
        }

        private static String decodeUnicodeEscapes(String text) {
            var matcher = UNICODE_ESCAPE.matcher(text);
            StringBuilder decoded = new StringBuilder(text.length());
            while (matcher.find()) {
                matcher.appendReplacement(decoded, String.valueOf((char) Integer.parseInt(matcher.group(1), 16)));
            }
            matcher.appendTail(decoded);
            return decoded.toString();
        }

        private static String fallbackSummary(String name, String type) {
            return name + " " + type + " public API.";
        }

        private static String methodName(MethodTree method, String typeName) {
            return method.getName().contentEquals("<init>") ? typeName : method.getName().toString();
        }

        private static String methodSignature(MethodTree method, String typeName) {
            String name = methodName(method, typeName);
            String typeParameters = method.getTypeParameters().isEmpty()
                ? ""
                : method.getTypeParameters().stream().map(Object::toString).collect(Collectors.joining(", ", "<", "> "));
            String returnType = method.getReturnType() == null ? "" : method.getReturnType() + " ";
            String parameters = method.getParameters().stream()
                .map(parameter -> parameter.getType() + " " + parameter.getName())
                .collect(Collectors.joining(", "));
            String throwsClause = method.getThrows().isEmpty()
                ? ""
                : method.getThrows().stream().map(Object::toString).collect(Collectors.joining(", ", " throws ", ""));
            return (modifiers(method.getModifiers()) + " " + typeParameters + returnType + name + "(" + parameters + ")" + throwsClause)
                .replaceAll("\\s+", " ")
                .trim();
        }

        private static String fieldSignature(VariableTree variable) {
            return (modifiers(variable.getModifiers()) + " " + variable.getType() + " " + variable.getName())
                .replaceAll("\\s+", " ")
                .trim();
        }

        private static String nestedTypeSignature(ClassTree nested) {
            return (modifiers(nested.getModifiers()) + " " + typeOf(nested) + " " + nested.getSimpleName())
                .replaceAll("\\s+", " ")
                .trim();
        }

        private static String modifiers(ModifiersTree modifiers) {
            return modifiers.getFlags().stream()
                .map(modifier -> modifier.toString().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
        }
    }
}
