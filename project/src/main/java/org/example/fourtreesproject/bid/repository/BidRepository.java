package org.example.fourtreesproject.bid.repository;

import org.example.fourtreesproject.bid.model.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    @Query("SELECT b FROM Bid b WHERE b.groupBuy.idx = :gpbuyIdx AND (b.bidStatus = '등록' OR b.bidStatus = '수정') ORDER BY b.bidPrice DESC")
    List<Bid> findAllByGpbuyIdx(Long gpbuyIdx);
}
