package com.team8.account_device_management;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.team8.DateTime;
import com.team8.Meta_DB;
import com.team8.Meta_Page;
import com.team8.SecurityUtil;



/**
 * Servlet implementation class LoginHandler
 * 로그인을 수행합니다.
 * !! 미완성 !!
 */
@WebServlet("/LoginHandler")
public class LoginHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private Connection conn;
	private PreparedStatement check_signed_pstmt;
	private PreparedStatement check_login_pstmt;
	private PreparedStatement login_pstmt;
	private final Object lock = new Object();
	

    /**
     * Default constructor. 
     */
    public LoginHandler() {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void init() throws ServletException {
    	super.init();
    	try {
    		Class.forName(Meta_DB.driver);
    		conn = DriverManager.getConnection(
    				Meta_DB.db_url, Meta_DB.db_user, Meta_DB.db_password);
    		check_signed_pstmt = conn.prepareStatement(String.format(
    				"SELECT %s FROM %s WHERE %s=?",
    				Meta_DB.col_mbKey, Meta_DB.tb_member, Meta_DB.col_mbID));
    		check_login_pstmt = conn.prepareStatement(String.format(
    				"SELECT * FROM %s WHERE %s=?",
    				Meta_DB.tb_login, Meta_DB.col_mbKey));
    		login_pstmt = conn.prepareStatement(String.format(
    				"INSERT INTO %s VALUES (?,?,?)",
    				Meta_DB.tb_login));
    	}
    	catch(ClassNotFoundException e) {
    		throw new UnavailableException("Couldn't load database driver");
    	}
    	catch(SQLException e){
    		throw new UnavailableException("Couldn't get db connection");
    	}
    }
    
    @Override
    public void destroy() {
    	super.destroy();
    	try {
    		if(conn != null)
    			conn.close();
    	}
    	catch (SQLException ignore) { }
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// user의 id, password 가져옴
		String userID = request.getParameter("userID");
		String password = request.getParameter("password");
		
		// user의 유효성 체크
		if(!allowUser(userID, password)) {
			
		}
		else {
			// 유효한 로그인. 세션 생성
			HttpSession session = request.getSession(true);
			session.setAttribute("logon.done", userID);
			
			// 접근하려 했던 페이지가 있다면 경로 재설정
			try {
				String target = (String)session.getAttribute("login.target");
				if(target != null)
					response.sendRedirect(target);
				return;
			}
			catch(Exception ignored) {}
			
			// 없다면 메인 페이지로 이동.
			response.sendRedirect(String.format("%s://%s:%d%s",
					request.getScheme(), request.getServerName(), request.getServerPort(),
					Meta_Page.MAINPAGE));
		}
	}
	
	
	protected boolean allowUser(String userID, String password) {
		int mb_no = -1;
		String mb_hashedpswd;
		String mb_salt;
		ResultSet rs;
		
		try {
			synchronized (lock) {
				// 유저 ID 존재 여부 확인
				check_signed_pstmt.clearParameters();
				check_signed_pstmt.setString(1, userID);
				rs = check_signed_pstmt.executeQuery();
				if(rs.next()) {
					mb_no = rs.getInt(Meta_DB.col_mbKey);
					mb_hashedpswd = rs.getString(Meta_DB.col_mbHashPasswd);
					mb_salt = rs.getString(Meta_DB.col_mbSalt);
				}
				else {
					return false;
				}
				
				// Password 일치 여부 확인
				if(!SecurityUtil.encryptSHA256(password + mb_salt).equals(mb_hashedpswd))
					return false;
				
				// 로그인 여부 확인
				check_login_pstmt.clearParameters();
				check_login_pstmt.setInt(1, mb_no);
				rs = check_login_pstmt.executeQuery();
				if(rs.next()) {
					
				}
				else {
					return false;
				}
				
				// 로그인 진행
				login_pstmt.clearParameters();
				login_pstmt.setInt(1, mb_no);
				DateTime dt = new DateTime("Asia/Seoul");
				login_pstmt.setDate(2, dt.getDate());
				login_pstmt.setTime(3, dt.getTime());
				login_pstmt.executeUpdate();
				
				return true;
			}
		}
		catch (SQLException e) {

			return false;
		}
	}
}
