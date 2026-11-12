// Inventaire des routes de l'API, lu dans les classes compilees de l'application.
//
// Pour chaque route : verbes, chemin, acces exige (calcule par les vraies regles de securite),
// parametres, corps, reponse, et le schema JSON de chaque modele rencontre. Le resultat est un
// fichier JSON que render.py transforme en documentation. Lancement : voir README.md.

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.authorization.AdminApiAuthorizationManager;
import com.sni.bokaticowork.security.authorization.SecurityRoles;
import com.sni.bokaticowork.security.config.PublicPaths;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.*;

public class RouteCatalog {

    private static final String ROOT = "com.sni.bokaticowork";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final DefaultParameterNameDiscoverer NAMES = new DefaultParameterNameDiscoverer();
    private static final Set<String> SKIPPED = Set.of(
            "jakarta.servlet.http.HttpServletRequest", "jakarta.servlet.http.HttpServletResponse",
            "jakarta.servlet.http.HttpSession", "java.security.Principal",
            "org.springframework.security.core.Authentication", "org.springframework.validation.BindingResult",
            "org.springframework.ui.Model", "java.util.Locale", "org.springframework.web.util.UriComponentsBuilder",
            "org.springframework.web.context.request.WebRequest");

    private final AdminApiAuthorizationManager admin = new AdminApiAuthorizationManager();
    private final Method resolvePermission;
    private final Map<String, Schema> schemas = new TreeMap<>();
    private final Set<Class<?>> modelled = new HashSet<>();
    private final Map<String, List<String>> enums = new TreeMap<>();

    RouteCatalog() {
        resolvePermission = ReflectionUtils.findMethod(AdminApiAuthorizationManager.class, "resolvePermission",
                jakarta.servlet.http.HttpServletRequest.class);
        ReflectionUtils.makeAccessible(resolvePermission);
        // Deux modeles de meme nom dans deux paquets ne doivent pas s'ecraser.
        TypeNameResolver.std.setUseFqn(true);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage : RouteCatalog <fichier de sortie .json>");
            System.exit(2);
        }
        new RouteCatalog().run(new File(args[0]));
    }

    void run(File out) throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        List<Map<String, Object>> routes = new ArrayList<>();
        List<String> controllers = scanner.findCandidateComponents(ROOT).stream()
                .map(definition -> definition.getBeanClassName()).sorted().toList();
        for (String name : controllers) {
            Class<?> type = Class.forName(name);
            if (Modifier.isAbstract(type.getModifiers())) continue;
            routes.addAll(routesOf(type));
        }
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("controllers", controllers.size());
        catalog.put("routes", routes);
        catalog.put("adminRealmRoles", new TreeSet<>(SecurityRoles.ADMIN_REALM));
        catalog.put("publicPatterns", PublicPaths.patterns());
        catalog.put("publicReadPatterns", PublicPaths.readPatterns());
        catalog.put("schemas", schemas);
        catalog.put("enums", enums);
        ObjectMapper writer = Json.mapper().copy().enable(SerializationFeature.INDENT_OUTPUT);
        writer.writeValue(out, catalog);
        System.out.println(routes.size() + " routes, " + controllers.size() + " controleurs, " + schemas.size() + " modeles -> " + out);
    }

    private List<Map<String, Object>> routesOf(Class<?> type) {
        RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
        String[] prefixes = base == null || base.path().length == 0 ? new String[]{""} : base.path();
        boolean restController = AnnotatedElementUtils.hasAnnotation(type, ResponseBody.class);
        List<Map<String, Object>> routes = new ArrayList<>();
        for (Method method : ReflectionUtils.getUniqueDeclaredMethods(type, m -> !m.isBridge() && !m.isSynthetic())) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null) continue;
            String[] suffixes = mapping.path().length == 0 ? new String[]{""} : mapping.path();
            List<String> verbs = Arrays.stream(mapping.method()).map(Enum::name).toList();
            for (String prefix : prefixes) {
                for (String suffix : suffixes) {
                    String path = join(prefix, suffix);
                    Map<String, Object> route = new LinkedHashMap<>();
                    route.put("methods", verbs.isEmpty() ? List.of("GET", "POST", "PUT", "PATCH", "DELETE") : verbs);
                    route.put("path", path);
                    route.put("package", type.getPackageName().substring(ROOT.length() + 1));
                    route.put("controller", type.getSimpleName());
                    route.put("source", "src/main/java/" + type.getName().replace('.', '/') + ".java");
                    route.put("handler", method.getName());
                    route.put("declaredIn", method.getDeclaringClass().getSimpleName());
                    route.put("access", access(type, method, path, verbs.isEmpty() ? "GET" : verbs.get(0)));
                    if (mapping.consumes().length > 0) route.put("consumes", mapping.consumes());
                    if (mapping.produces().length > 0) route.put("produces", mapping.produces());
                    if (AnnotatedElementUtils.hasAnnotation(method, Deprecated.class)) route.put("deprecated", true);
                    com.sni.bokaticowork.core.idempotency.aop.Idempotent idempotent =
                            AnnotatedElementUtils.findMergedAnnotation(method, com.sni.bokaticowork.core.idempotency.aop.Idempotent.class);
                    if (idempotent != null) {
                        Map<String, Object> idempotency = new LinkedHashMap<>();
                        idempotency.put("header", idempotent.keyHeader());
                        idempotency.put("operation", idempotent.operation());
                        idempotency.put("required", idempotent.required());
                        route.put("idempotency", idempotency);
                    }
                    boolean body = restController || AnnotatedElementUtils.hasAnnotation(method, ResponseBody.class);
                    describeParameters(type, method, route);
                    route.put("response", body ? describe(ResolvableType.forMethodReturnType(method, type)) : "page HTML (vue serveur)");
                    routes.add(route);
                }
            }
        }
        return routes;
    }

    private static String join(String prefix, String suffix) {
        String path = (prefix + (suffix.isEmpty() || suffix.startsWith("/") || prefix.endsWith("/") ? "" : "/") + suffix);
        return path.isEmpty() ? "/" : path.replaceAll("//+", "/");
    }

    // L'ordre reproduit SecurityConfig : la premiere regle qui correspond decide.
    private Map<String, Object> access(Class<?> type, Method method, String path, String verb) {
        Map<String, Object> access = new LinkedHashMap<>();
        PreAuthorize guard = Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class))
                .orElse(AnnotatedElementUtils.findMergedAnnotation(type, PreAuthorize.class));
        if (guard != null) access.put("preAuthorize", guard.value());
        String sample = path.replaceAll("\\{[^}]+}", "x1");
        String v1 = ApiPath.V1;
        if (PublicPaths.matches(verb, sample) || ("GET".equals(verb) && MATCHER.match(v1 + "/documents/*/signed-preview", sample))) {
            access.put("kind", "public");
        } else if (sample.startsWith(v1 + "/client/") || sample.equals(v1 + "/client")
                || sample.equals(v1 + "/portal/kyc") || sample.startsWith(v1 + "/portal/kyc/")) {
            access.put("kind", "member-portal");
        } else if (("GET".equals(verb) && MATCHER.match(v1 + "/document-types/*/upload-config", sample))
                || sample.equals(v1 + "/auth/me") || sample.equals(v1 + "/auth/logout")) {
            access.put("kind", "authenticated");
        } else if (sample.startsWith(v1 + "/")) {
            access.put("kind", "admin");
            MockHttpServletRequest request = new MockHttpServletRequest(verb, sample);
            String permission = (String) ReflectionUtils.invokeMethod(resolvePermission, admin, request);
            access.put("permission", permission == null ? "ADMIN:ACCESS" : permission);
        } else {
            access.put("kind", "authenticated");
        }
        return access;
    }

    private void describeParameters(Class<?> type, Method method, Map<String, Object> route) {
        List<Map<String, Object>> params = new ArrayList<>();
        for (int index = 0; index < method.getParameterCount(); index++) {
            MethodParameter parameter = new MethodParameter(method, index).withContainingClass(type);
            parameter.initParameterNameDiscovery(NAMES);
            ResolvableType resolved = ResolvableType.forMethodParameter(parameter);
            Class<?> raw = resolved.toClass();
            if (SKIPPED.contains(raw.getName()) || parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    || raw.getName().startsWith("com.sni.bokaticowork.security.") && raw.getSimpleName().contains("Principal")) {
                continue;
            }
            Map<String, Object> param = new LinkedHashMap<>();
            PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
            RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
            RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
            RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
            RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
            String fallback = parameter.getParameterName();
            if (pathVariable != null) {
                param.put("in", "path");
                param.put("name", first(pathVariable.name(), first(pathVariable.value(), fallback)));
                param.put("required", true);
            } else if (requestBody != null) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("type", describe(resolved));
                body.put("required", requestBody.required());
                route.put("body", body);
                continue;
            } else if (requestPart != null || MultipartFile.class.isAssignableFrom(raw) || isFileList(resolved)) {
                param.put("in", "multipart");
                param.put("name", requestPart != null ? first(requestPart.name(), first(requestPart.value(), fallback))
                        : requestParam != null ? first(requestParam.name(), first(requestParam.value(), fallback)) : fallback);
                param.put("required", requestPart != null ? requestPart.required() : requestParam == null || requestParam.required());
            } else if (requestHeader != null) {
                param.put("in", "header");
                param.put("name", first(requestHeader.name(), first(requestHeader.value(), fallback)));
                param.put("required", requestHeader.required() && ValueConstants.DEFAULT_NONE.equals(requestHeader.defaultValue()));
            } else if (Pageable.class.isAssignableFrom(raw)) {
                PageableDefault defaults = parameter.getParameterAnnotation(PageableDefault.class);
                param.put("in", "pageable");
                param.put("name", "page, size, sort");
                if (defaults != null) {
                    param.put("default", "size=" + defaults.size() + (defaults.sort().length > 0
                            ? ", sort=" + String.join(",", defaults.sort()) + "," + defaults.direction() : ""));
                }
            } else if (Sort.class.isAssignableFrom(raw)) {
                param.put("in", "query");
                param.put("name", "sort");
            } else if (requestParam != null || isSimple(resolved)) {
                param.put("in", "query");
                param.put("name", requestParam != null ? first(requestParam.name(), first(requestParam.value(), fallback)) : fallback);
                boolean optional = raw == Optional.class || requestParam == null
                        || !requestParam.required() || !ValueConstants.DEFAULT_NONE.equals(requestParam.defaultValue());
                param.put("required", !optional);
                if (requestParam != null && !ValueConstants.DEFAULT_NONE.equals(requestParam.defaultValue())) {
                    param.put("default", requestParam.defaultValue());
                }
            } else {
                // Un objet sans annotation : ses champs se passent en parametres de requete.
                param.put("in", "query-object");
                param.put("name", fallback);
            }
            param.put("type", describe(resolved));
            params.add(param);
        }
        route.put("params", params);
    }

    private static boolean isFileList(ResolvableType type) {
        return Collection.class.isAssignableFrom(type.toClass())
                && MultipartFile.class.isAssignableFrom(type.getGeneric(0).toClass());
    }

    private static boolean isSimple(ResolvableType type) {
        Class<?> raw = type.toClass();
        if (Collection.class.isAssignableFrom(raw) || raw == Optional.class) return isSimple(type.getGeneric(0));
        return raw.isPrimitive() || raw.isEnum() || CharSequence.class.isAssignableFrom(raw)
                || Number.class.isAssignableFrom(raw) || raw == Boolean.class || raw == UUID.class
                || Temporal.class.isAssignableFrom(raw) || raw == BigDecimal.class;
    }

    private static String first(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    /** Le type lisible, et chaque modele de l'application qu'il touche ajoute aux schemas. */
    private String describe(ResolvableType type) {
        Class<?> raw = type.toClass();
        if (raw == Object.class && type.getType() instanceof java.lang.reflect.WildcardType) return "?";
        if (raw == void.class || raw == Void.class) return "void";
        if (raw.isArray()) return raw == byte[].class ? "binary" : describe(type.getComponentType()) + "[]";
        String name = raw.getName().startsWith(ROOT) ? raw.getName().replace('$', '.') : raw.getSimpleName();
        if (raw.isEnum()) {
            enums.putIfAbsent(name, Arrays.stream(raw.getEnumConstants()).map(value -> ((Enum<?>) value).name()).toList());
            return name;
        }
        ResolvableType[] generics = type.getGenerics();
        if (raw.getName().startsWith(ROOT) && !raw.getName().contains(".templateResponse.")) {
            model(raw);
        }
        if (generics.length == 0) return name;
        StringJoiner inner = new StringJoiner(", ", name + "<", ">");
        for (ResolvableType generic : generics) inner.add(describe(generic));
        return inner.toString();
    }

    /**
     * Les reponses passent par Jackson 3, que le lecteur de schemas ignore : un champ qu'il met en
     * texte doit l'etre aussi dans la doc.
     */
    private static void applyJackson3(String name, Schema schema) {
        Class<?> type = classOf(name);
        if (type == null || schema.getProperties() == null) return;
        for (java.lang.reflect.Field field : type.getDeclaredFields()) {
            // Swagger decrit de la meme facon un Instant (UTC) et un LocalDateTime (sans fuseau).
            Schema<?> property = (Schema<?>) schema.getProperties().get(field.getName());
            if (property != null && field.getType() == java.time.LocalDateTime.class) {
                property.setFormat("local-date-time");
            }
            tools.jackson.databind.annotation.JsonSerialize serialize =
                    field.getAnnotation(tools.jackson.databind.annotation.JsonSerialize.class);
            if (type.isRecord()) {
                for (java.lang.reflect.RecordComponent component : type.getRecordComponents()) {
                    if (component.getName().equals(field.getName()) && serialize == null) {
                        serialize = component.getAnnotation(tools.jackson.databind.annotation.JsonSerialize.class);
                    }
                }
            }
            if (serialize != null && serialize.using() == tools.jackson.databind.ser.std.ToStringSerializer.class
                    && schema.getProperties().containsKey(field.getName())) {
                schema.getProperties().put(field.getName(), new io.swagger.v3.oas.models.media.StringSchema()
                        .description("nombre envoyé en texte"));
            }
        }
    }

    private static Class<?> classOf(String name) {
        String candidate = name;
        while (true) {
            try {
                return Class.forName(candidate);
            } catch (ClassNotFoundException missing) {
                int dot = candidate.lastIndexOf('.');
                if (dot < 0) return null;
                candidate = candidate.substring(0, dot) + '$' + candidate.substring(dot + 1);
            }
        }
    }

    private void model(Class<?> raw) {
        if (!modelled.add(raw)) return;
        try {
            Map<String, Schema> found = ModelConverters.getInstance().readAll(new AnnotatedType(raw));
            found.forEach(RouteCatalog::applyJackson3);
            schemas.putAll(found);
        } catch (RuntimeException | LinkageError error) {
            System.err.println("schema illisible : " + raw.getName() + " (" + error + ")");
        }
    }
}
