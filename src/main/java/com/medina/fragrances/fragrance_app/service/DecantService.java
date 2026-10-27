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
        return decantRepository.save(decant);
    }

    @Transactional
    public void eliminar(Integer id) {
        decantRepository.deleteById(id);
    }
}
