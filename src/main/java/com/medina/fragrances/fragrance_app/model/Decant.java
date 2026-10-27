package com.medina.fragrances.fragrance_app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "decant")
public class Decant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "marca", length = 100, nullable = false)
    private String marca;

    @Column(name = "tamano", length = 20, nullable = false)
    private String tamano = "3ml";

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    public String getPrecioFormateado() {
        return String.format("S/. %.2f", precio == null ? 0.0 : precio);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getTamano() { return tamano; }
    public void setTamano(String tamano) { this.tamano = tamano; }
    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
