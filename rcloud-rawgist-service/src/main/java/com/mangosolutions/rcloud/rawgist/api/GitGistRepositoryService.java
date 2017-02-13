package com.mangosolutions.rcloud.rawgist.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;

public class GitGistRepositoryService implements GistRepositoryService {

	private static final String RECYCLE_FOLDER_NAME = ".recycle";

	private File repositoryRoot;
	private File recycleRoot;
	private GistIdGenerator idGenerator;
	private HazelcastInstance hazelcastInstance;

	private ObjectMapper objectMapper;

	public GitGistRepositoryService(String repositoryRoot, GistIdGenerator idGenerator,
			HazelcastInstance hazelcastInstance, ObjectMapper objectMapper) throws IOException {
		this.repositoryRoot = new File(repositoryRoot);
		if (!this.repositoryRoot.exists()) {
			FileUtils.forceMkdir(this.repositoryRoot);
		}
		recycleRoot = new File(repositoryRoot, RECYCLE_FOLDER_NAME);
		if (!this.recycleRoot.exists()) {
			FileUtils.forceMkdir(this.recycleRoot);
		}
		this.idGenerator = idGenerator;
		this.hazelcastInstance = hazelcastInstance;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<GistResponse> listGists() {
		List<GistResponse> gists = new ArrayList<GistResponse>();
		for(File file: FileUtils.listFiles(repositoryRoot, FileFilterUtils.and(FileFileFilter.FILE, new NameFileFilter(GitGistRepository.GIST_META_JSON_FILE)), TrueFileFilter.INSTANCE)) {
			GistRepository repository = new GitGistRepository(file.getParentFile(), objectMapper);
			gists.add(repository.getGist());
		}
		return gists;
	}

	@Override
	public GistResponse getGist(String gistId) {
		Lock lock = hazelcastInstance.getLock(gistId);
		try {
			if (lock.tryLock(10, TimeUnit.SECONDS)) {
				try {
					File repositoryFolder = getRepositoryFolder(gistId);
					GistRepository repository = new GitGistRepository(repositoryFolder, gistId, objectMapper);
					return repository.getGist();
				} finally {
					lock.unlock();
				}
			} else {
				throw new RuntimeException("Could not acquire write lock for gist " + gistId);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Could not acquire write lock for gist " + gistId);
		}
	}

	@Override
	public GistResponse createGist(GistRequest request) {
		String gistId = idGenerator.generateId();
		File repositoryFolder = getRepositoryFolder(gistId);
		GistRepository repository = new GitGistRepository(repositoryFolder, gistId, objectMapper);
		return repository.createGist(request);
	}

	@Override
	public GistResponse editGist(String gistId, GistRequest request) {
		Lock lock = hazelcastInstance.getLock(gistId);
		try {
			if (lock.tryLock(10, TimeUnit.SECONDS)) {
				try {
					File repositoryFolder = getRepositoryFolder(gistId);
					GistRepository repository = new GitGistRepository(repositoryFolder, gistId, objectMapper);
					return repository.editGist(request);
				} finally {
					lock.unlock();
				}
			} else {
				throw new RuntimeException("Could not acquire write lock for gist " + gistId);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Could not acquire write lock for gist " + gistId);
		}
	}

	@Override
	public void deleteGist(String gistId) {
		Lock lock = hazelcastInstance.getLock(gistId);
		try {
			if (lock.tryLock(10, TimeUnit.SECONDS)) {
				try {
					File repositoryFolder = getRepositoryFolder(gistId);
					FileUtils.moveDirectoryToDirectory(repositoryFolder, new File(recycleRoot, gistId), true);
					FileUtils.forceDelete(repositoryFolder);
				} catch (IOException e) {
					throw new RuntimeException("Could not delete gist.", e);
				} finally {
					lock.unlock();
				}
			} else {
				throw new RuntimeException("Could not acquire write lock for gist " + gistId);
			}

		} catch (InterruptedException e) {
			throw new RuntimeException("Could not acquire write lock for gist " + gistId);
		}
	}

	private File getRepositoryFolder(String id) {
		String[] paths = id.split("-");
		File folder = repositoryRoot;
		for (String path : paths) {
			folder = new File(folder, path);
		}
		return folder;
	}

}
