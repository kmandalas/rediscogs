package com.redislabs.rediscogs;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.redisearch.Document;
import io.redisearch.Query;
import io.redisearch.SearchResult;
import io.redisearch.Suggestion;
import io.redisearch.client.SuggestionOptions;

@RestController
class RediscogsController {

	@Autowired
	private RediscogsConfiguration config;

	@Autowired
	private RediSearchClientConfiguration rediSearchConfig;

	@Autowired
	private MasterRepository repository;

	@Autowired
	private ImageRepository imageRepository;

	private Optional<RedisMaster> getRedisMaster(Document doc) {
		String id = doc.getId();
		if (doc.getId() != null && doc.getId().length() > 0) {
			return repository.findById(id);
		}
		return Optional.empty();
	}

	@GetMapping("/suggest-artists")
	public Stream<String> suggestArtists(@RequestParam("prefix") String prefix) {
		SuggestionOptions options = SuggestionOptions.builder().fuzzy().build();
		List<Suggestion> results = rediSearchConfig.getClient(config.getArtistsSuggestionIdx()).getSuggestion(prefix,
				options);
		return results.stream().map(result -> result.getString());
	}

	@GetMapping("/search-albums")
	public Stream<RedisMaster> searchAlbums(@RequestParam("query") String queryString) {
		Query query = new Query(queryString);
		query.limit(0, config.getSearchResultsLimit());
		SearchResult results = rediSearchConfig.getClient(config.getMastersIndex()).search(query);
		return results.docs.stream().map(doc -> getRedisMaster(doc)).filter(Optional::isPresent).map(Optional::get);
	}

	@ResponseBody
	@GetMapping(value = "/album-image/{id}")
	public ResponseEntity<byte[]> getImageAsResource(@PathVariable("id") String masterId) throws IOException {
		final HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(CacheControl.noCache().getHeaderValue());
		return new ResponseEntity<>(imageRepository.getImage(masterId), headers, HttpStatus.OK);
	}

}