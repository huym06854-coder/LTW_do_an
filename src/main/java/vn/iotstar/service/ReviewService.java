package vn.iotstar.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.Review;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.repository.*;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final ProductService products;
    private final UserService users;
    private final OrderRepository orders;

    public ReviewService(ReviewRepository reviews, ProductService products, UserService users, OrderRepository orders) {
        this.reviews = reviews;
        this.products = products;
        this.users = users;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<Review> forProduct(Long id) {
        return reviews.findByProductIdOrderByCreatedAtDesc(id);
    }

    @Transactional
    public Review submit(Long userId, Long productId, int rating, String comment) {
        if (rating < 1 || rating > 5)
            throw new BusinessException("Đánh giá phải từ 1 đến 5 sao");
        if (reviews.findByUserIdAndProductId(userId, productId).isPresent())
            throw new BusinessException("Bạn đã đánh giá sản phẩm này");
        // Require purchase history (completed delivery) before leaving a review.
        if (orders.countCompletedPurchases(userId, productId, vn.iotstar.domain.enums.OrderStatus.COMPLETED) == 0)
            throw new BusinessException("Chỉ khách đã nhận sản phẩm mới được đánh giá");
        Review r = new Review();
        r.setUser(users.byId(userId));
        r.setProduct(products.findActive(productId));
        r.setRating(rating);
        r.setComment(comment);
        return reviews.save(r);
    }
}
