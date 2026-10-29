package com.medina.fragrances.fragrance_app.service;

import com.medina.fragrances.fragrance_app.model.Decant;
import com.medina.fragrances.fragrance_app.repository.DecantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DecantService {

    private final DecantRepository decantRepository;

    public DecantService(DecantRepository decantRepository) {
        this.decantRepository = decantRepository;
    }

    @Transactional(readOnly = true)
    public List<Decant> listarTodos() {
        return decantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Decant> buscarPorId(Integer id) {
        return decantRepository.findById(id);
    }

    @Transactional
    public Decant guardar(Decant decant) {
        if (decant.getImageUrl() != null && !decant.getImageUrl().isBlank()) {
            String url = decant.getImageUrl().trim();
            if (!url.startsWith("/") && !url.startsWith("http://") && !url.startsWith("https://")) {
                decant.setImageUrl("/" + url);
            }
        }
        if (decant.getTamano() == null || decant.getTamano().isBlank()) {
            decant.setTamano("3ml - 10ml");
        }
        return decantRepository.save(decant);
    }

    @Transactional
    public void eliminar(Integer id) {
        decantRepository.deleteById(id);
    }

    // Carga inicial: si la tabla esta vacia, inserta los 8 decants
    @Transactional
    public void cargarDecantsIniciales() {
        if (decantRepository.count() == 0) {
            List<Decant> iniciales = List.of(
                crearDecant("Valentino Uomo Intense", "Valentino", "3ml - 10ml", 45.0, 15, "/img/Valentino Uomo.webp", "Fragancia elegante y sofisticada para el hombre moderno."),
                crearDecant("JPG Le Male", "JEAN PAUL GAULTIER", "3ml - 10ml", 20.0, 20, "/img/JEAN PAUL GAULTIER LE MALE.webp", "Un clásico atemporal con notas de lavanda y vainilla."),
                crearDecant("Azzaro 9PM", "AZZARO", "3ml - 10ml", 60.0, 15, "/img/9pm.webp", "Frescura masculina radical con notas de bergamota."),
                crearDecant("Lattafa Khamrah Dukhan", "LATTAFA", "3ml - 10ml", 75.0, 12, "/img/khamrah dukhan.webp", "Aroma dulce, especiado y adictivo."),
                crearDecant("Vintage Radio", "MONTALE", "3ml - 10ml", 55.0, 10, "/img/Vintage Radio.webp", "Fragancia retro con un toque moderno."),
                crearDecant("Asad Bourbon", "LATTAFA", "3ml - 10ml", 65.0, 14, "/img/Asad Bourbon.webp", "Poderoso y masculino con notas de tabaco."),
                crearDecant("Amethyst", "MONTALE", "3ml - 10ml", 50.0, 8, "/img/Amethyst.webp", "Elegancia y misterio en un frasco."),
                crearDecant("Shaheen Gold", "LATTAFA", "3ml - 10ml", 60.0, 10, "/img/Shaheen Gold.webp", "Un aroma dorado y sofisticado.")
            );
            decantRepository.saveAll(iniciales);
            System.out.println(">>> Decants iniciales cargados en la base de datos: " + iniciales.size());
        }
    }

    private Decant crearDecant(String nombre, String marca, String tamano, Double precio, Integer stock, String imageUrl, String descripcion) {
        Decant d = new Decant();
        d.setNombre(nombre);
        d.setMarca(marca);
        d.setTamano(tamano);
        d.setPrecio(precio);
        d.setStock(stock);
        d.setImageUrl(imageUrl);
        d.setDescripcion(descripcion);
        return d;
    }
}
