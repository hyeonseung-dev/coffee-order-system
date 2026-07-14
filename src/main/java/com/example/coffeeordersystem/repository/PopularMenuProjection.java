package com.example.coffeeordersystem.repository;

/** 인기 메뉴 집계 쿼리가 반환하는 필요한 필드만 정의한다. */
public interface PopularMenuProjection {

	Long getMenuId();

	String getName();

	Long getOrderCount();
}
