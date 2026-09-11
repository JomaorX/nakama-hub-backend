package com.nakamahub.backend.controllers;

import com.nakamahub.backend.services.SitemapService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mapa del sitio para los buscadores.
 *
 * Lo sirve el backend porque es el único que sabe qué posts existen y cuáles son
 * públicos. Sin esto, Google tiene que descubrir cada publicación siguiendo
 * enlaces desde la portada, que sólo muestra las últimas.
 */
@RestController
public class SitemapController {

    private final SitemapService sitemapService;

    public SitemapController(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        return sitemapService.build();
    }
}
