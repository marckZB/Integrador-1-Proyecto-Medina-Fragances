package com.medina.fragrances.fragrance_app;

import com.medina.fragrances.fragrance_app.service.DecantService;
import com.medina.fragrances.fragrance_app.service.ProductoService;
import com.medina.fragrances.fragrance_app.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FragranceAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(FragranceAppApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(ProductoService productoService,
									 DecantService decantService,
									 UsuarioService usuarioService) {
		return args -> {
			productoService.cargarProductosIniciales();
			decantService.cargarDecantsIniciales();
			usuarioService.asegurarAdminPrincipal();
		};
	}
}
