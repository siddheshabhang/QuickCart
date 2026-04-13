package com.quickcart.config;

import com.quickcart.entity.Product;
import com.quickcart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    @Bean
    CommandLineRunner seedProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                log.info("Products already seeded — skipping.");
                return;
            }

            List<Product> products = List.of(
                    Product.builder()
                            .name("Amul Full Cream Milk")
                            .description("Fresh full cream milk, 1 litre pack.")
                            .price(68.00)
                            .stock(200)
                            .build(),
                    Product.builder()
                            .name("Aashirvaad Atta")
                            .description("Whole wheat flour, 5 kg bag. Ideal for rotis.")
                            .price(299.00)
                            .stock(150)
                            .build(),
                    Product.builder()
                            .name("Tata Salt")
                            .description("Iodised sea salt, 1 kg pack.")
                            .price(24.00)
                            .stock(500)
                            .build(),
                    Product.builder()
                            .name("Fortune Sunflower Oil")
                            .description("Refined sunflower cooking oil, 1 litre bottle.")
                            .price(135.00)
                            .stock(120)
                            .build(),
                    Product.builder()
                            .name("Britannia Good Day Butter Cookies")
                            .description("Crispy butter biscuits, 200 g pack.")
                            .price(35.00)
                            .stock(300)
                            .build(),
                    Product.builder()
                            .name("Maggi 2-Minute Noodles")
                            .description("Instant masala noodles, pack of 4 (280 g total).")
                            .price(60.00)
                            .stock(250)
                            .build(),
                    Product.builder()
                            .name("Tata Tea Gold")
                            .description("Premium blended tea, 500 g pack.")
                            .price(285.00)
                            .stock(180)
                            .build(),
                    Product.builder()
                            .name("Nescafé Classic Instant Coffee")
                            .description("Smooth instant coffee, 100 g jar.")
                            .price(249.00)
                            .stock(100)
                            .build(),
                    Product.builder()
                            .name("Surf Excel Easy Wash")
                            .description("Detergent powder for tough stains, 1 kg pack.")
                            .price(120.00)
                            .stock(90)
                            .build(),
                    Product.builder()
                            .name("Colgate StrongTeeth Toothpaste")
                            .description("Cavity protection toothpaste, 200 g tube.")
                            .price(89.00)
                            .stock(220)
                            .build()
            );

            productRepository.saveAll(products);
            log.info("✅ Seeded {} dummy products.", products.size());
        };
    }
}
