package com.julius.resame.repository;

import com.julius.resame.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByEndDateBeforeAndStatusIn(LocalDateTime endDate, List<Book.Status> statuses);

    List<Book> findByUserUserId(Long userId);
    
}
