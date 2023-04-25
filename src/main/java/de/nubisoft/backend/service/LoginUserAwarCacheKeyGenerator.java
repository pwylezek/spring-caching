package de.nubisoft.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * By default {@link org.springframework.cache.interceptor.SimpleKeyGenerator} relies only on passed parameters to method
 * annotated by @Cacheable. However, sometimes business logic methods itself rely only on logged user id additionally,
 * so default behavior is not sufficient.
 * For that reason, we want to also take into account logged user id to build cache key.
 */
@Service
@Slf4j
class LoginUserAwareCacheKeyGenerator extends SimpleKeyGenerator {

    private final LoginService loginService;

    @Autowired
    LoginUserAwareCacheKeyGenerator(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        var extendedParamsAboutLoggedUserId = new ArrayList<>();
        extendedParamsAboutLoggedUserId.add(loginService.getLoggedDoctorId());
        extendedParamsAboutLoggedUserId.addAll(Arrays.stream(params).toList());
        return super.generate(target, method, extendedParamsAboutLoggedUserId);
    }
}

