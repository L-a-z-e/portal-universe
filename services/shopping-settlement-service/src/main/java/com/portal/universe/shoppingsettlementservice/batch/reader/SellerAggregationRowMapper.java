package com.portal.universe.shoppingsettlementservice.batch.reader;

import com.portal.universe.shoppingsettlementservice.batch.dto.SellerAggregation;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SellerAggregationRowMapper implements RowMapper<SellerAggregation> {

    @Override
    public SellerAggregation mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SellerAggregation(
                rs.getLong("seller_id"),
                rs.getBigDecimal("total_sales"),
                rs.getBigDecimal("total_refunds"),
                rs.getInt("order_count")
        );
    }
}
