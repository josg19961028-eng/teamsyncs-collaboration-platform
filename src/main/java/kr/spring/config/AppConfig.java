package kr.spring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import kr.spring.users.security.GoogleAdditionalInfoInterceptor;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AppConfig implements WebMvcConfigurer {

    private final GoogleAdditionalInfoInterceptor googleAdditionalInfoInterceptor;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/main/resultView").setViewName("thviews/common/resultView");
        registry.addViewController("/main/resultError").setViewName("thviews/common/resultError");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(googleAdditionalInfoInterceptor)
                .addPathPatterns(
                        "/main/home",
                        "/team/**",
                        "/calendar/**",
                        "/kanban/**",
                        "/minutes/**",
                        "/chat/**",
                        "/storage/**",
                        "/notice/**",
                        "/notification/**",
                        "/bot/**",
                        "/member/myPage",
                        "/member/todos",
                        "/member/notifications",
                        "/member/changePassword"
                )
                .excludePathPatterns(
                        "/assets/**",
                        "/member/login",
                        "/member/logout",
                        "/users/google/additional-info",
                        "/oauth2/**",
                        "/login/oauth2/**"
                );
    }
}
