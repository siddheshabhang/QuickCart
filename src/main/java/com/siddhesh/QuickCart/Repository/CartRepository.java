package com.siddhesh.QuickCart.Repository;

import com.siddhesh.QuickCart.Entity.Cart;
import com.siddhesh.QuickCart.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}
