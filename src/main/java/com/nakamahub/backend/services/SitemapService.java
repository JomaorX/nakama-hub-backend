package com.nakamahub.backend.services;

import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SitemapService {

    /**
     * Tope de entradas. El formato admite cincuenta mil por fichero; mucho antes de
     * acercarse a esa cifra habrá que paginar el mapa en varios ficheros con índice.
     */
    private static final int MAX_ENTRIES = 5000;

    private static final DateTimeFormatter W3C = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final String siteOrigin;

    public SitemapService(PostRepository postRepository,
                          UserRepository userRepository,
                          @Value("${app.site-origin}") String siteOrigin) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.siteOrigin = siteOrigin.replaceAll("/+$", "");
    }

    @Transactional(readOnly = true)
    public String build() {
        StringBuilder xml = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        append(xml, "/", null, "daily", "1.0");

        // Sólo lo publicado y público: el mapa no puede delatar borradores.
        List<Post> posts = postRepository
                .findVisibleFor(null, PageRequest.of(0, MAX_ENTRIES, Sort.by("createdAt").descending()))
                .getContent();

        for (Post post : posts) {
            append(xml, "/post/" + post.getId(),
                    post.getUpdatedAt() == null ? null : post.getUpdatedAt().atOffset(ZoneOffset.UTC).format(W3C),
                    "weekly", "0.8");
        }

        for (User user : userRepository.findPublicProfiles(PageRequest.of(0, MAX_ENTRIES))) {
            append(xml, "/u/" + user.getUsername(), null, "weekly", "0.5");
        }

        return xml.append("</urlset>\n").toString();
    }

    private void append(StringBuilder xml, String path, String lastModified,
                        String changeFrequency, String priority) {
        xml.append("  <url>\n")
                .append("    <loc>").append(escape(siteOrigin + path)).append("</loc>\n");

        if (lastModified != null) {
            xml.append("    <lastmod>").append(lastModified).append("</lastmod>\n");
        }

        xml.append("    <changefreq>").append(changeFrequency).append("</changefreq>\n")
                .append("    <priority>").append(priority).append("</priority>\n")
                .append("  </url>\n");
    }

    /** El nombre de usuario va en la URL y admite guiones bajos, pero no está de más. */
    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
