package de.nubisoft.backend.service;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.nubisoft.backend.configuration.CachingConfiguration;
import de.nubisoft.backend.domain.Patient;
import de.nubisoft.backend.domain.PatientDocumentsResponse;
import de.nubisoft.backend.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
@Slf4j
public class PatientsService {

    private final LoginService loginService;

    private static final Set<Patient> patients = Set.of(

            // First doctor
            new Patient("1", "1"),
            new Patient("1", "2"),
            new Patient("1", "3"),
            new Patient("1", "4"),
            new Patient("1", "5"),
            new Patient("1", "6"),
            new Patient("1", "7"),
            new Patient("1", "8"),
            new Patient("1", "9"),
            new Patient("1", "10"),

            // Second doctor
            new Patient("2", "11"),
            new Patient("2", "12"),
            new Patient("2", "13"),
            new Patient("2", "14"),
            new Patient("2", "15"),
            new Patient("2", "16"),
            new Patient("2", "17"),
            new Patient("2", "18"),
            new Patient("2", "19"),
            new Patient("2", "20")
    );

    private final Map<String, List<String>> patientsDocumentCacheV2 = new ConcurrentHashMap<>();

    private final Cache<String, List<String>> patientsDocumentCacheV3 = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(5)
            .build();
    private final RedisTemplate<String, List<String>> patientsDocumentCacheV4;

    @Autowired
    public PatientsService(LoginService loginService, RedisConnectionFactory connectionFactory) {
        this.loginService = loginService;

        this.patientsDocumentCacheV4 = new RedisTemplate<>();
        this.patientsDocumentCacheV4.setConnectionFactory(connectionFactory);
        this.patientsDocumentCacheV4.setKeySerializer(new StringRedisSerializer());
        this.patientsDocumentCacheV4.afterPropertiesSet();

    }

    /**
     * Get patient documents without caching mechanism
     *
     * @param patientId
     * @return List<PatientDocument>
     */
    public PatientDocumentsResponse getPatientDocumentsV1(String patientId) {
        log.info("Fetching patient documents in V1 implementation. Patient id: {}", patientId);
        var patient = getPatientByLoggedDoctorOrThrowNotFound(patientId);
        return new PatientDocumentsResponse(patient, fetchPatientDocumentsFromVerySlowExternalService(patientId));
    }


    /**
     * Get patient documents with caching mechanism based on java.util.concurrent.ConcurrentHashMap and programmatic approach
     * <p>
     * PROS:
     * - Almost zero entry level.
     * - Uses embedded in Java SDK so none additional library is needed.
     * - Zero additional components in the architecture needed.
     * <p>
     * CONS:
     * - Uses RAM memory in order to store cache data (which is always limited)
     * - Using RAM as the data storage may be problematic with bigger datasets.
     * - Memory it is occupied as long as the application is running or the entry in Map is deleted.
     * - Cache has to be manually evicted (if necessary).
     * - Cache state is not kept between application restarts
     * <p>
     *
     * @param patientId
     * @return List<PatientDocument>
     */
    public PatientDocumentsResponse getPatientDocumentsV2(String patientId) {
        log.info("Fetching patient documents in V2 implementation. Patient id: {}", patientId);
        var patient = getPatientByLoggedDoctorOrThrowNotFound(patientId);
        var cachedOrFetchedDocuments = patientsDocumentCacheV2.computeIfAbsent(patientId, this::fetchPatientDocumentsFromVerySlowExternalService);
        return new PatientDocumentsResponse(patient, cachedOrFetchedDocuments);
    }

    /**
     * Get patient documents with caching mechanism based on Caffeine library and programmatic approach
     * <p>
     * PROS:
     * - Entry level similar to V2 approach.
     * - Cache can be automatically evicted after specific period of time or when too much memory is consumed.
     * - Zero additional components in the architecture needed.
     * <p>
     * CONS:
     * - Uses RAM memory in order to store cache data (which is always limited)
     * - Using RAM as the data storage may be problematic with bigger datasets.
     * - Cache state is not kept between application restarts
     * <p>
     *
     * @param patientId
     * @return List<PatientDocument>
     */
    public PatientDocumentsResponse getPatientDocumentsV3(String patientId) {
        log.info("Fetching patient documents in V3 implementation. Patient id: {}", patientId);
        var patient = getPatientByLoggedDoctorOrThrowNotFound(patientId);
        var cachedOrFetchedDocuments = patientsDocumentCacheV3.get(patientId, this::fetchPatientDocumentsFromVerySlowExternalService);
        return new PatientDocumentsResponse(patient, cachedOrFetchedDocuments);
    }

    /**
     * Get patient documents with caching mechanism based on Redis Data library and programmatic approach
     * <p>
     * PROS:
     * - Cache can be automatically evicted after specific period of time.
     * - Does not use RAM memory in order to store cache data.
     * - Cache is kept between application restarts
     * - Cache is shared between different instances of the same microservice (especially important when horizontal scaling is enabled)
     * <p>
     * CONS:
     * - Slightly higher entry level than in V2 and V3 (Redis Spring SDK knowledge required)
     * - One (some cache storage provider, e.g. Redis) additional component in the architecture needed.
     * <p>
     *
     * @param patientId
     * @return List<PatientDocument>
     */
    public PatientDocumentsResponse getPatientDocumentsV4(String patientId) {
        log.info("Fetching patient documents in V4 implementation. Patient id: {}", patientId);
        var patient = getPatientByLoggedDoctorOrThrowNotFound(patientId);
        var cachedDocuments = patientsDocumentCacheV4.opsForValue().get(patientId);
        if (!CollectionUtils.isEmpty(cachedDocuments)) {
            return new PatientDocumentsResponse(patient, cachedDocuments);
        }
        var fetchedDocuments = fetchPatientDocumentsFromVerySlowExternalService(patientId);
        patientsDocumentCacheV4.opsForValue().set(patientId, fetchedDocuments, Duration.ofMinutes(15));
        return new PatientDocumentsResponse(patient, fetchedDocuments);
    }

    /**
     * Get patient documents with caching mechanism based on Redis library and aspect oriented programming (AOP) approach
     * <p>
     * PROS:
     * - Almost zero-code approach
     * - Function code does not anything about caching mechanism (except annotations)
     * - Caching can be disabled at any time without function body modification
     * - Cache can be automatically evicted after specific period of time.
     * - Does not use RAM memory in order to store cache data.
     * - Cache is kept between application restarts
     * - Cache is shared between different instances of the same microservice (especially important when horizontal scaling is enabled)
     * <p>
     * CONS:
     * - One (some cache storage provider, e.g. Redis) additional component in the architecture needed.
     * - In non-standalone, multi tenant applications we have to care about potential data leaks as the key is built based on method params only.
     * <p>
     *
     * @param patientId
     * @return List<PatientDocument>
     */
    @Cacheable(value = CachingConfiguration.PATIENTS_DOCUMENTS_CACHE_NAME_V5)
    public PatientDocumentsResponse getPatientDocumentsV5(String patientId) {
        log.info("Fetching patient documents in V5 implementation. Patient id: {}", patientId);
        var patient = getPatientByLoggedDoctorOrThrowNotFound(patientId);
        return new PatientDocumentsResponse(patient,
                fetchPatientDocumentsFromVerySlowExternalService(patientId));
    }

    /**
     * Get patient documents with caching mechanism based on Redis library and aspect oriented programming (AOP) approach
     * <p>
     * PROS:
     * - Almost zero-code approach
     * - Function code does not anything about caching mechanism (except annotations)
     * - Caching can be disabled at any time without function body modification
     * - Cache can be automatically evicted after specific period of time.
     * - Does not use RAM memory in order to store cache data.
     * - Cache is kept between application restarts
     * - Cache is shared between different instances of the same microservice (especially important when horizontal scaling is enabled)
     * - Cache key is aware about logged user id so for every user cache is built independently.
     * <p>
     * CONS:
     * - One (some cache storage provider, e.g. Redis) additional component in the architecture needed.
     * - In non-standalone, multi tenant applications we have to care about potential data leaks as the key is built based on method params only.
     * <p>
     *
     * @param patientId
     * @return List<PatientDocument>
     */
    @Cacheable(value = CachingConfiguration.PATIENTS_DOCUMENTS_CACHE_NAME_V6, keyGenerator = "loginUserAwareCacheKeyGenerator")
    public PatientDocumentsResponse getPatientDocumentsV6(String patientId) {
        log.info("Fetching patient documents in V6 implementation. Patient id: {}", patientId);
        var patient = getPatientByLoggedDoctorOrThrowNotFound(patientId);
        return new PatientDocumentsResponse(patient,
                fetchPatientDocumentsFromVerySlowExternalService(patientId));
    }


    /**
     * Get patient documents with caching mechanism based on Redis library and aspect oriented programming (AOP) approach
     * BUT WITH A BUG!
     * <p>
     * BUG explanation:
     * AOP (aspect oriented programming) in Spring works based on proxies. Proxy is not applied during accessing function within same class, field etc.
     * Therefore, caching mechanism is not applied here (but function works perfectly fine though).
     *
     * @param patientId
     * @return List<PatientDocument>
     */
    public PatientDocumentsResponse getPatientDocumentsV7(String patientId) {
        return getPatientDocumentsV6(patientId);
    }

    private Patient getPatientByLoggedDoctorOrThrowNotFound(String patientId) {
        var loggedDoctorId = loginService.getLoggedDoctorId();
        return patients.stream()
                .filter(it -> Objects.equals(it.doctorId(), loggedDoctorId) && Objects.equals(it.patientId(), patientId))
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }


    private List<String> fetchPatientDocumentsFromVerySlowExternalService(String patientId) {

        try {
            log.info("Fetching patient documents from external service. Patient id: {}", patientId);
            String documentContent = new String(new ClassPathResource("document.txt").getInputStream().readAllBytes());
            List<String> documents = new ArrayList<>();
            IntStream.range(0, 15).forEach(id ->
                    Mono.just(id)
                            .delayElement(Duration.ofMillis(100))
                            .doOnNext(it -> documents.add(patientId + documentContent + UUID.randomUUID()))
                            .block()
            );
            return documents;
        } catch (Exception unexpected) {
            throw new RuntimeException(unexpected);
        }
    }
}
