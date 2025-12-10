package com.example.tracking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class FileVisitorRepository implements VisitorRepository {

	private final Path dataFile = Paths.get("data/visitors.json");
	private final Path lockFile = Paths.get("data/visitors.lock");
	private final ObjectMapper objectMapper = new ObjectMapper();

	public FileVisitorRepository() {
		try {
			if (dataFile.getParent() != null && !Files.exists(dataFile.getParent())) {
				Files.createDirectories(dataFile.getParent());
			}
			if (!Files.exists(dataFile)) {
				Files.write(dataFile, "[]".getBytes());
			}
			if (lockFile.getParent() != null && !Files.exists(lockFile.getParent())) {
				Files.createDirectories(lockFile.getParent());
			}
			if (!Files.exists(lockFile)) {
				Files.createFile(lockFile);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize data file", e);
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws IOException;
	}

	private void withLock(ThrowingRunnable task) {
		try (RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
	     FileChannel ch = raf.getChannel();
	     FileLock lock = ch.lock()) {
			task.run();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<VisitorInfo> readAll() {
		final List<VisitorInfo>[] holder = new List[1];
		withLock(() -> {
			byte[] bytes = Files.readAllBytes(dataFile);
			if (bytes.length == 0) holder[0] = new ArrayList<>();
			else holder[0] = objectMapper.readValue(bytes, new TypeReference<List<VisitorInfo>>() {});
		});
		return holder[0];
	}

	private void writeAll(List<VisitorInfo> list) {
		withLock(() -> {
			byte[] bytes = objectMapper.writeValueAsBytes(list);
			Files.write(dataFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		});
	}

	@Override
	public void save(VisitorInfo v) {
		List<VisitorInfo> all = readAll();
		if (v.getId() == null || v.getId().isEmpty()) {
			v.setId(UUID.randomUUID().toString());
		}
		all.add(v);
		writeAll(all);
	}

	@Override
	public List<VisitorInfo> findAll() {
		return readAll();
	}

	@Override
	public VisitorInfo findByShortCode(String shortCode) {
		for (VisitorInfo v : readAll()) {
			if (shortCode != null && shortCode.equals(v.getShortCode())) {
				return v;
			}
		}
		return null;
	}

	@Override
	public void clearAll() {
		withLock(() -> {
			Files.write(dataFile, "[]".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		});
	}
}
