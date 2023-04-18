package de.nubisoft.backend.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


public class SecurityFilter extends OncePerRequestFilter {

    static final String USER_TOKEN_HEADER_NAME = "User-Token";
    static final String USER_TOKEN_PARAMETER_NAME = "userToken";

    private final SecurityContextRepository securityContextRepository;

    public SecurityFilter(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userToken = request.getHeader(USER_TOKEN_HEADER_NAME);
        if (userToken == null) {
            userToken = request.getParameter(USER_TOKEN_PARAMETER_NAME);
        }

        // We always want to clear SecurityContextHolder and perform authentication only when user & token are specified
        SecurityContextHolder.clearContext();
        if (docplannerUserTokenIsNotBlankAndIsValid(userToken)) {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(getUserFromUserToken(userToken),
                    null, List.of()));
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
        }

        filterChain.doFilter(request, response);
    }


    private static String getUserFromUserToken(String userToken) {
        return userToken.split("\\.", 2)[0];
    }

    private static String getTokenFromUserToken(String userToken) {
        return userToken.split("\\.", 2)[1];
    }

    private static boolean docplannerUserTokenIsNotBlankAndIsValid(String userToken) {
        return userToken != null &&
                userToken.matches(".+\\..+") &&
                getTokenFromUserToken(userToken).equals("auth_token_for_user_".concat(getUserFromUserToken(userToken)));
    }
}
