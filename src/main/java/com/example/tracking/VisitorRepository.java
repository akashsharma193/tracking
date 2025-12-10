package com.example.tracking;

import java.util.List;

public interface VisitorRepository {
	void save(VisitorInfo v);

	List<VisitorInfo> findAll();

	VisitorInfo findByShortCode(String shortCode);

	void clearAll();
}
