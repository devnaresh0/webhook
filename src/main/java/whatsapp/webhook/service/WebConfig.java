//package whatsapp.webhook.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Autowired
//    private LicenseInterceptor licenseInterceptor;
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//
//        registry.addInterceptor(licenseInterceptor)
//                .addPathPatterns("/**")
//                .excludePathPatterns(
//                        "/api/license/activate",
//                        "/webhook",
//                        "/error"
//                );
//    }
//}