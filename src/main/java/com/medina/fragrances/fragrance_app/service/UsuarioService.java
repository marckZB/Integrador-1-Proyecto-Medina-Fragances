package com.medina.fragrances.fragrance_app.service;

import com.medina.fragrances.fragrance_app.model.Usuario;
import com.medina.fragrances.fragrance_app.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Registrar un nuevo usuario
    @Transactional
    public Usuario registrar(Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
            usuario.setRol("CLIENTE");
        }
        if (!esPasswordBCrypt(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario guardar(Usuario usuario) {
        if (!esPasswordBCrypt(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario asegurarAdminPrincipal() {
        Optional<Usuario> adminExistente = usuarioRepository.findFirstByRol("ADMIN");
        if (adminExistente.isPresent()) {
            return asegurarPasswordCifrado(adminExistente.get());
        }

        Optional<Usuario> existente = usuarioRepository.findByEmail("jahirortizbr@gmail.com")
                .or(() -> usuarioRepository.findByEmail("jahirotzbr@gmail.com"))
                .or(() -> usuarioRepository.findByEmail("jahirortzbr@gmail.com"))
                .or(() -> usuarioRepository.findByDni("76615558"));

        if (existente.isPresent()) {
            Usuario admin = existente.get();
            if (!"ADMIN".equals(admin.getRol())) {
                admin.setRol("ADMIN");
            }
            return asegurarPasswordCifrado(admin);
        }

        Usuario admin = new Usuario();
        admin.setNombre("Jahir Ortiz");
        admin.setEmail("jahirortizbr@gmail.com");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setTelefono("999888777");
        admin.setDni("76615558");
        admin.setDireccion("La Planicie");
        admin.setRol("ADMIN");
        return usuarioRepository.save(admin);
    }

    // Buscar usuario por email (para login)
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    // Verificar credenciales para login: email y password deben coincidir
    @Transactional(readOnly = true)
    public boolean verificarLogin(String email, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            return passwordEncoder.matches(password, usuario.getPassword());
        }
        return false;
    }

    // Verificar si el email ya esta registrado
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    // Verificar si el DNI ya esta registrado
    @Transactional(readOnly = true)
    public boolean existeDni(String dni) {
        return usuarioRepository.existsByDni(dni);
    }

    // Listar todos los usuarios
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    private Usuario asegurarPasswordCifrado(Usuario usuario) {
        if (!esPasswordBCrypt(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            return usuarioRepository.save(usuario);
        }
        return usuario;
    }

    private boolean esPasswordBCrypt(String password) {
        return password != null
                && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    /**
     * Actualiza los datos editables del perfil del cliente.
     * NO toca password (eso se hace en cambiarPassword).
     */
    @Transactional
    public Usuario actualizarPerfil(Integer id, String nombre, String telefono, String direccion) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));

        usuario.setNombre(nombre);
        usuario.setTelefono(telefono);
        usuario.setDireccion(direccion);
        // email, dni, password y rol se preservan intactos
        return usuarioRepository.save(usuario);
    }

    /**
     * Cambia la contrasena del usuario verificando primero la actual.
     * Devuelve true si el cambio se realizo, false si la contrasena actual no
     * coincide.
     */
    @Transactional
    public boolean cambiarPassword(Integer id, String passwordActual, String passwordNueva) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            return false;
        }

        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
        return true;
    }

    /**
     * Compara una contrasena en texto plano contra la almacenada (BCrypt).
     */
    @Transactional(readOnly = true)
    public boolean verificarPassword(Usuario usuario, String passwordRaw) {
        if (usuario == null || usuario.getPassword() == null || passwordRaw == null) {
            return false;
        }
        return passwordEncoder.matches(passwordRaw, usuario.getPassword());
    }

}
