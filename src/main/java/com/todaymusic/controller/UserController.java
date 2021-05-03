package com.todaymusic.controller;



import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.todaymusic.config.auth.dto.SessionUser;

import com.todaymusic.service.UserService;
import com.todaymusic.service.PlaylistService;

@Controller
public class UserController {
	

	private final HttpSession session;
	
	@Autowired
	public UserController(HttpSession session) {
		this.session=session;
	}
	
	@GetMapping("/")
	public String main(Model model) {
		SessionUser user = (SessionUser) session.getAttribute("user");
		if (user != null) {
			return "redirect:/mylist";
		}
		return "index";
	}


	
//	@PostMapping("/login")
//	public String login(@Valid MemberForm memberForm, BindingResult result, Model model) {
//		/*
//		 * 입력하지 않은 칸이 존재할 때 입력을 요구하는 에러메세지 출력
//		 */
//		if (result.hasErrors()) {
//			return "member/login";
//		}
//		
//		
//		/*
//		 * memberForm에서 받은 데이터로 member 생성
//		 */
//		Member member = new Member();
//		member.setName(memberForm.getName());
//		
//		
//		/*
//		 * 입력받은 member 정보가 올바르지 않을 때 가입되지 않은 유저명이거나 잘못된 비밀번호라는 에러메세지 출력
//		 */
//		if (memberService.login(member)==false) {
//			model.addAttribute("inval", "가입되지 않은 유저이거나, 잘못된 비밀번호입니다.");
//			return "member/login";
//		}else {
//			session.setAttribute("MEMBER", memberForm);
//			return "redirect:/";
//		}
//		
//	}

	
	
}