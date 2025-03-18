    package com.ticket.desk_cartel.security;

    import com.ticket.desk_cartel.services.UserService;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.stereotype.Component;
    import org.springframework.web.filter.OncePerRequestFilter;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.io.IOException;
    import java.util.List;

    /**
     * JWT filter that intercepts incoming HTTP requests to validate JWT tokens.
     * <p>
     * This filter extracts the token from the Authorization header, validates it,
     * and if valid, sets the authentication in the SecurityContext.
     * </p>
     */
    @Component
    public class JwtFilter extends OncePerRequestFilter {

        private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

        private final JwtUtil jwtUtil;
        private final UserService userService;

        // Define endpoint patterns that do not require JWT processing.
        private final List<String> excludedPaths = List.of(
                "/auth/login",
                "/auth/register",
                "/auth/verify",
                "/error",
                "/swagger-ui",
                "/v3/api-docs",
                "/ws-chat",
                "/ws-chat-sockjs"
        );


        public JwtFilter(JwtUtil jwtUtil, UserService userService) {
            this.jwtUtil = jwtUtil;
            this.userService = userService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String requestURI = request.getRequestURI();  // e.g. "/ws-chat-sockjs/info"
            logger.debug("Request URI: {}", requestURI);
            excludedPaths.forEach(ep -> logger.debug("Excluded path: {}", ep));
            boolean isExcluded = excludedPaths.stream()
                    .anyMatch(path -> requestURI.startsWith(path));
            logger.debug("Is Excluded? {}", isExcluded);

            String username = jwtUtil.extractUsername(token);
            logger.info("👤 Extracted Username: {}", username);
            String role = jwtUtil.extractRole(token);
            logger.info("👤 Extracted Role: {}", role);
            // Only proceed if the user is not already authenticated.
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(username);
                if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                    logger.info("✅ Token is valid for user: {}", username);

                }
            }
            
            if (isExcluded) {
                logger.debug("Skipping JWT filter for WebSocket path: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                logger.info("🔐 Extracted Token: {}", token);

                String username = jwtUtil.extractUsername(token);
                logger.info("👤 Extracted Username: {}", username);

                // Only proceed if the user is not already authenticated.
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userService.loadUserByUsername(username);
                    if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                        logger.info("✅ Token is valid for user: {}", username);

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        logger.warn("❌ Token validation failed for user: {}", username);
                    }
                }
            } else {
                logger.warn("🔐 No valid Authorization header found for request: {}", requestURI);
            }

            filterChain.doFilter(request, response);
        }
    }
