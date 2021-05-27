package com.myplaylists.controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.myplaylists.config.auth.dto.SessionUser;
import com.myplaylists.domain.Bookmark;
import com.myplaylists.domain.Playlist;
import com.myplaylists.domain.Song;
import com.myplaylists.domain.User;
import com.myplaylists.dto.YoutubeForm;
import com.myplaylists.service.PlaylistService;
import com.myplaylists.service.SongService;
import com.myplaylists.service.UserService;
import com.myplaylists.service.YoutubeService;

@Controller
public class PlaylistController {
	
	private final PlaylistService playlistService;
	private final UserService userService;
	private final YoutubeService youtubeService;
	private final SongService songService;
	private final HttpSession session;
	
	
	@Autowired
	public PlaylistController(PlaylistService playlistService, UserService userService, YoutubeService youtubeService, SongService songService, HttpSession session) {
		this.playlistService = playlistService;
		this.userService = userService;
		this.youtubeService = youtubeService;
		this.songService = songService;
		this.session = session;
	}
	
	@GetMapping("/mylist")
	public String mylist(Model model, @PageableDefault(size=6, sort="updatedAt",direction=Sort.Direction.DESC)Pageable pageable) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user!=null) {
			Page<Playlist> playlists = playlistService.findMyPlaylists(pageable, userService.findUser(user));
			model.addAttribute("playlists", playlists);
			
			model.addAttribute("isFirst", playlists.isFirst());
			model.addAttribute("isLast", playlists.isLast());
			model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
			model.addAttribute("next", pageable.next().getPageNumber());
			
			
			return "playlist/mylist";
		}
		return "index";
	}
	
	@GetMapping("/playlist/search")
	public String search(Model model, String keyword, @PageableDefault(size=6, sort="updatedAt",direction=Sort.Direction.DESC)Pageable pageable) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		Page<Playlist> playlists = playlistService.search(pageable,keyword, userService.findUser(user));
		
		model.addAttribute("playlists", playlists);
		
		model.addAttribute("isFirst", playlists.isFirst());
		model.addAttribute("isLast", playlists.isLast());
		model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
		model.addAttribute("next", pageable.next().getPageNumber());
		return "playlist/searchPlaylist";
	}
	
	@GetMapping("/playlist/add")
	public String add(Model model) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user!=null) {
			model.addAttribute("playlist", new Playlist());
			return "playlist/addPlaylist";
		}
		return "index";
		
	}
	
	@PostMapping("/playlist/add")
	public String add(@Valid Playlist playlist, BindingResult result) {
		if (result.hasErrors()) {
			return "playlist/addPlaylist";
		}
		SessionUser user = (SessionUser) session.getAttribute("user");
		
		playlist.setMember(userService.findUser(user));
		playlistService.addPlaylist(playlist);
		return "redirect:/mylist";
	}


	@GetMapping("/playlist/{playlistId}")
	public String detail(@PathVariable("playlistId") Long playlistId, Model model) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user!=null) {
			
			
			Playlist playlist = playlistService.getPlaylist(playlistId);
			
			model.addAttribute("playlist",playlist);
			model.addAttribute("author",userService.getAuthor(playlist.getUser().getId()));
			model.addAttribute("songs", songService.findSongs(playlist));
			model.addAttribute("isBookmark",playlistService.validateBookmark(user.getId(), playlistId).isPresent());
			
			return "playlist/detail";
		}
		return "user/login";
	}
	
	@DeleteMapping("/playlist/{playlistId}")
	public String deletePlaylist(@PathVariable("playlistId") Long playlistId) {
		Playlist playlist = playlistService.getPlaylist(playlistId);
		playlistService.deletePlaylist(playlist);
		return "redirect:/mylist";
	}
	
	
	@GetMapping("/playlist/{playlistId}/add")
	public String addsong(@PathVariable("playlistId") Long id, Model model) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user!=null) {
			
			/*
			 * 해당 플레이리스트 객체화 & 해당 플레이리스트의 노래 페이징 처리
			 */
			Playlist playlist = playlistService.getPlaylist(id);
			model.addAttribute("playlist",playlist);
			model.addAttribute("author",userService.getAuthor(playlist.getUser().getId()));

			model.addAttribute("songs", songService.findSongs(playlist));


			return "playlist/addSong";
		}
		return "index";
	}
	
	
	@GetMapping("/playlist/{playlistId}/youtube_search")
	public String youtubeSearch(@PathVariable("playlistId") Long playlistId, String search, Model model) throws IOException, ParseException {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user!=null) {
			/*
			 * 해당 플레이리스트 객체화 & 해당 플레이리스트의 노래 페이징 처리
			 */
			Playlist playlist = playlistService.getPlaylist(playlistId);
			model.addAttribute("playlist",playlist);
			model.addAttribute("author",userService.getAuthor(playlist.getUser().getId()));
			model.addAttribute("songs", songService.findSongs(playlist));
	
			
			/*
			 * 유튜브검색서비스
			 */
			List<YoutubeForm> result = youtubeService.search(search);
			model.addAttribute("result", result);
			
			return "playlist/addSong";
		}
		return "index";
	}
	
	@PostMapping("/playlist/{playlistId}/{videoTitle}/{videoId}")
	public String addSong(@PathVariable("playlistId") Long playlistId, @PathVariable("videoTitle") String videoTitle, @PathVariable("videoId") String videoId) {
		Playlist playlist = playlistService.getPlaylist(playlistId);
		Song song = new Song();
		song.setTitle(videoTitle);
		song.setVideoId(videoId);
		playlist.addSong(song);
		songService.saveSong(song);
		return "redirect:/playlist/{playlistId}";
	}
	
	
	
	@GetMapping("/playlist/{playlistId}/{songId}")
	public String playSong(@PathVariable("playlistId") Long playlistId, @PathVariable("songId") Long songId, Model model) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user!=null) {
			
			/*
			 * 해당 플레이리스트 객체화 & 해당 플레이리스트의 노래 페이징 처리
			 */
			Playlist playlist = playlistService.getPlaylist(playlistId);
			Song song = songService.getSong(songId);
			model.addAttribute("playlist",playlist);
			model.addAttribute("author",userService.getAuthor(playlist.getUser().getId()));
			model.addAttribute("nowSong",song);
			model.addAttribute("songs", songService.findSongs(playlist));
			model.addAttribute("isBookmark",playlistService.validateBookmark(user.getId(), playlistId).isPresent());
			
			return "playlist/playSong";
		}
		return "index";
	}
	
	@GetMapping("/playlist/{playlistId}/{songId}/update")
	public String updateSong(Model model, @PathVariable("playlistId") Long playlistId, @PathVariable("songId") Long songId) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user != null) {
			/*
			 * 해당 플레이리스트 객체화 & 해당 플레이리스트의 노래 페이징 처리
			 */
			Playlist playlist = playlistService.getPlaylist(playlistId);
			Song song = songService.getSong(songId);
			model.addAttribute("playlist",playlist);
			model.addAttribute("author",userService.getAuthor(playlist.getUser().getId()));
			model.addAttribute("nowSong",song);
			model.addAttribute("songs", songService.findSongs(playlist));
			model.addAttribute("isBookmark",playlistService.validateBookmark(user.getId(), playlistId).isPresent());
			
			return "playlist/updateSong";
		}
		return "index";
	}
	
	@PutMapping("/playlist/{playlistId}/{songId}/update")
	public String updateSong(String title, @PathVariable("songId") Long songId) {
		Song song = songService.getSong(songId);
		song.update(title);
		songService.saveSong(song);
		return "redirect:/playlist/{playlistId}/{songId}";
	}
	
	@DeleteMapping("/playlist/{playlistId}/{songId}")
	public String deleteSong(@PathVariable("playlistId") Long playlistId, @PathVariable("songId") Long songId) {
		Song song = songService.getSong(songId);
		songService.deleteSong(song);
		return "redirect:/playlist/{playlistId}";
	}
	
	
	@GetMapping("/all")
	public String playlists(@PageableDefault(size=6, sort="updatedAt",direction=Sort.Direction.DESC)Pageable pageable,Model model) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user!=null) {
			Page<Playlist> playlists = playlistService.findAllPlaylists(pageable);
			
			model.addAttribute("playlists", playlists);
			
			model.addAttribute("isFirst", playlists.isFirst());
			model.addAttribute("isLast", playlists.isLast());
			model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
			model.addAttribute("next", pageable.next().getPageNumber());
			return "playlist/playlists";
		}
		
		return "index";
	}
	
	@ResponseBody
	@PostMapping("/playlist/{playlistId}/bookmark")
	public void bookmark(@PathVariable("playlistId") Long playlistId, User user) {
		/*
		 * 해당 플레이리스트가 이미 즐겨찾기 되어있는지 확인
		 */
		Optional<Bookmark> result = playlistService.validateBookmark(user.getId(), playlistId);
		
		// 이미 즐겨찾기가 되어있으면 즐겨찾기 삭제
		if (result.isPresent()) {
			playlistService.deleteBookmark(result.get());
		}
		// 즐겨찾기가 되어있지 않으면 즐겨찾기 설정
		else {
			Bookmark bookmark = new Bookmark();
			bookmark.setUser(user);
			bookmark.setPlaylistId(playlistId);
			playlistService.setBookmark(bookmark);
		}
	}
	
	@GetMapping("/bookmark")
	public String bookmark(@PageableDefault(size=6, sort="createdAt",direction=Sort.Direction.DESC)Pageable pageable, Model model) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user!=null) {
			List<Playlist> playlists = playlistService.findBookmarkPlaylists(pageable, userService.findUser(user));
			
			model.addAttribute("playlists", playlists);

//			model.addAttribute("isFirst", playlists.isFirst());
//			model.addAttribute("isLast", playlists.isLast());
			model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
			model.addAttribute("next", pageable.next().getPageNumber());
			return "playlist/bookmark";
		}
		
		return "index";
	}
}
