package com.whatiread.catalog.author.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.catalog.author.domain.Author;
import com.whatiread.catalog.author.domain.BookAuthor;
import com.whatiread.catalog.author.repository.AuthorRepository;
import com.whatiread.catalog.author.repository.BookAuthorRepository;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.repository.BookRepository;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthorApiIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookAuthorRepository bookAuthorRepository;

    @Autowired
    private BookRepository bookRepository;

    private AuthSession user;
    private Author author;

    @BeforeEach
    void setUp() throws Exception {
        user = registerUser();
        author = authorRepository.findBySlug("octavia-butler")
                .orElseGet(() -> authorRepository.save(new Author("octavia-butler", "Octavia Butler")));
        Book book = bookRepository.findAll().stream()
                .filter(existing -> existing.getTitle().startsWith("Author Book"))
                .findFirst()
                .orElseGet(() -> {
                    Book created = new Book();
                    created.setTitle("Author Book " + UUID.randomUUID());
                    created.setAuthors(List.of("Octavia Butler"));
                    return bookRepository.save(created);
                });
        if (!bookAuthorRepository.existsById(new BookAuthor.BookAuthorId(book.getId(), author.getId()))) {
            bookAuthorRepository.save(new BookAuthor(book, author, 0));
        }
        addToLibrary(user, book.getId());
    }

    @Test
    void authorProfileIsPublic() throws Exception {
        mockMvc.perform(get(ApiPaths.AUTHORS + "/{slug}", author.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Octavia Butler"));
    }

    @Test
    void manualBookCreateLinksAuthorProfile() throws Exception {
        AuthSession fresh = registerUser();
        String body = """
                {"title":"Linked Author Book","authors":["Jane Author"],"pageCount":100}
                """;
        mockMvc.perform(post(ApiPaths.BOOKS)
                        .with(bearer(fresh.accessToken()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ApiPaths.AUTHORS + "/jane-author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Author"));
    }

    @Test
    void authorProfileBooksAndLibrary() throws Exception {
        mockMvc.perform(get(ApiPaths.AUTHORS + "/{slug}", author.getSlug())
                        .with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Octavia Butler"));

        mockMvc.perform(get(ApiPaths.AUTHORS + "/{slug}/books", author.getSlug())
                        .with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get(ApiPaths.AUTHORS + "/{slug}/library", author.getSlug())
                        .with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
