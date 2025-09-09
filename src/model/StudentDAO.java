package model;

import util.DBUtil;
import vo.StudentVO;
import vo.PersonVO;

import java.sql.*;
import java.util.ArrayList;

/**
 * {@code StudentDAO} 클래스는 학생 데이터를 관리하기 위한 데이터 접근 객체(DAO)입니다.
 * 데이터베이스에 연결되어 학생 데이터를 추가, 삭제, 수정, 검색, 정렬하는 기능을 제공합니다.
 * <p>
 * 이 클래스는 다음과 같은 주요 기능을 포함합니다:
 * - 데이터베이스 연결 및 쿼리 실행
 * - 학생 데이터 추가, 변경, 삭제 및 정렬
 * - 합계, 평균, 등급 계산
 */
public class StudentDAO implements Student {
    /**
     * 싱글톤(Singleton)으로 구현된 DAO 인스턴스
     */
    private static StudentDAO dao;

    /**
     * 생성자를 private으로 설정하여, 외부에서의 객체 생성을 제한합니다.
     */
    private StudentDAO() {
    }

    /**
     * DAO 인스턴스를 반환하는 싱글톤(Singleton) 메서드
     *
     * @return {@code StudentDAO} 인스턴스
     */
    public static StudentDAO getInstance() {
        if (dao == null) dao = new StudentDAO();
        return dao;
    }

    /**
     * 학생 데이터 관리 리스트
     */
    private ArrayList<StudentVO> studentlist = new ArrayList<>();
    private Connection conn;
    private PreparedStatement pstmt;
    private Statement stmt;
    private ResultSet rs;

    /**
     * 데이터베이스 연결 종료 메서드
     */
    private void disConnect() {
        try { if (rs != null) rs.close(); } catch (SQLException e) {}
        try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
        try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
        try { if (conn != null) conn.close(); } catch (SQLException e) {}
    }

    /**
     * 데이터베이스 연결 및 학생 데이터 읽어오기
     */
    private void connect() {
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM student";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                StudentVO studentVO = new StudentVO();
                studentVO.setSno(rs.getString("sno"));
                studentVO.setName(rs.getString("name"));
                studentVO.setKorean(Math.max(0, Math.min(100, rs.getInt("korean"))));
                studentVO.setEnglish(Math.max(0, Math.min(100, rs.getInt("english"))));
                studentVO.setMath(Math.max(0, Math.min(100, rs.getInt("math"))));
                studentVO.setScience(Math.max(0, Math.min(100, rs.getInt("science"))));

                this.total(studentVO);
                this.average(studentVO);
                this.grade(studentVO);

                studentlist.add(studentVO);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disConnect();
        }
    }

    /**
     * 학생 데이터를 추가합니다.
     *
     * @param personVO 추가할 학생 데이터
     */
    @Override
    public void input(PersonVO personVO) {
        StudentVO newStudent = (StudentVO) personVO;

        if (studentlist.isEmpty()) this.connect();

        try {
            conn = DBUtil.getConnection();
            String sql = "INSERT INTO STUDENT (SNO, NAME, KOREAN, ENGLISH, MATH, SCIENCE) VALUES (?, ?, ?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newStudent.getSno());
            pstmt.setString(2, newStudent.getName());
            pstmt.setInt(3, newStudent.getKorean());
            pstmt.setInt(4, newStudent.getEnglish());
            pstmt.setInt(5, newStudent.getMath());
            pstmt.setInt(6, newStudent.getScience());

            int result = pstmt.executeUpdate();
            if (result != 0) {
                this.total(newStudent);
                this.average(newStudent);
                this.grade(newStudent);
                studentlist.add(newStudent);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disConnect();
        }
    }

    /**
     * 학생 데이터를 수정합니다.
     *
     * @param personVO 수정할 학생 데이터 객체
     */
    @Override
    public void update(PersonVO personVO) {
        StudentVO student = (StudentVO) personVO;

        if (studentlist.isEmpty()) this.connect();

        try {
            conn = DBUtil.getConnection();
            String sql = "UPDATE Student SET NAME=?, KOREAN=?, ENGLISH=?, MATH=?, SCIENCE=? WHERE SNO=?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getName());
            pstmt.setInt(2, student.getKorean());
            pstmt.setInt(3, student.getEnglish());
            pstmt.setInt(4, student.getMath());
            pstmt.setInt(5, student.getScience());
            pstmt.setString(6, student.getSno());

            int result = pstmt.executeUpdate();
            if (result != 0) {
                this.total(student);
                this.average(student);
                this.grade(student);

                for (StudentVO s : studentlist) {
                    if (s.getSno().equals(student.getSno())) {
                        s.setName(student.getName());
                        s.setKorean(student.getKorean());
                        s.setEnglish(student.getEnglish());
                        s.setMath(student.getMath());
                        s.setScience(student.getScience());
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disConnect();
        }
    }

    /**
     * 학생 데이터를 삭제합니다.
     *
     * @param deleteNum 삭제할 학생의 ID
     */
    @Override
    public void delete(String deleteNum) {
        if (studentlist.isEmpty()) this.connect();

        try {
            conn = DBUtil.getConnection();
            String sql = "DELETE FROM student WHERE sno = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, deleteNum);

            int result = pstmt.executeUpdate();
            if (result != 0) {
                studentlist.removeIf(s -> s.getSno().equals(deleteNum));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            disConnect();
        }
    }

    /**
     * 전체 학생 데이터를 특정 조건에 따라 정렬하여 출력합니다.
     *
     * @param sortNum 정렬 조건을 나타내는 번호
     */
    public void totalSearch(int sortNum) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = createSortPreparedStatement(conn, sortNum);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("학번\t이름\t국어\t영어\t수학\t과학\t총점");
            System.out.println("--------------------------------------------------");
            while (rs.next()) {
                String sno = rs.getString("sno");
                String name = rs.getString("name");
                int korean = rs.getInt("korean");
                int english = rs.getInt("english");
                int math = rs.getInt("math");
                int science = rs.getInt("science");
                int total = korean + english + math + science;

                System.out.printf("%s\t%s\t%d\t%d\t%d\t%d\t%d\n",
                        sno, name, korean, english, math, science, total);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PreparedStatement createSortPreparedStatement(Connection conn, int sortNum) throws SQLException {
        String sql = "SELECT * FROM student";
        switch (sortNum) {
            case 1 -> sql += " ORDER BY name ASC";
            case 2 -> sql += " ORDER BY sno ASC";
            case 3 -> sql += " ORDER BY (korean + english + math + science) DESC";
        }
        return conn.prepareStatement(sql);
    }

    /**
     * 특정 학생 데이터를 검색하여 출력합니다.
     *
     * @param searchNum 검색할 학생의 ID
     */
    @Override
    public void search(String searchNum) {
        boolean found = false;
        for (StudentVO s : studentlist) {
            if (s.getSno().equals(searchNum)) {
                int total = s.getKorean() + s.getEnglish() + s.getMath() + s.getScience();
                System.out.printf("%s\t%s\t%d\t%d\t%d\t%d\t%d\n",
                        s.getSno(), s.getName(),
                        s.getKorean(), s.getEnglish(), s.getMath(), s.getScience(),
                        total);
                found = true;
                break;
            }
        }
        if (!found) System.out.println("입력된 학생이 없습니다.");
    }

    /**
     * 학생 데이터를 정렬합니다.
     *
     * @param sortNum 정렬 조건을 나타내는 번호
     */
    @Override
    public void sort(int sortNum) {
        switch (sortNum) {
            case 1 -> studentlist.sort((s1, s2) -> s1.getName().compareTo(s2.getName()));
            case 2 -> studentlist.sort((s1, s2) -> s1.getSno().compareTo(s2.getSno()));
            case 3 -> studentlist.sort((s1, s2) -> s2.getTotal() - s1.getTotal());
        }
    }

    /**
     * 학생의 총점을 계산합니다.
     *
     * @param studentVO 총점을 계산할 학생 객체
     */
    public void total(StudentVO studentVO) {
        studentVO.setTotal(studentVO.getKorean() + studentVO.getEnglish() + studentVO.getMath() + studentVO.getScience());
    }

    /**
     * 학생의 평균 점수를 계산합니다.
     *
     * @param studentVO 평균 점수를 계산할 학생 객체
     */
    @Override
    public void average(StudentVO studentVO) {
        studentVO.setAverage(studentVO.getTotal() / 4.0);
    }

    /**
     * 학생의 등급을 결정합니다.
     *
     * @param studentVO 등급을 계산할 학생 객체
     */
    @Override
    public void grade(StudentVO studentVO) {
        double avg = studentVO.getAverage();
        if (avg >= 90) studentVO.setGrade("A");
        else if (avg >= 80) studentVO.setGrade("B");
        else if (avg >= 70) studentVO.setGrade("C");
        else if (avg >= 60) studentVO.setGrade("D");
        else studentVO.setGrade("F");
    }

    @Override
    public void input(StudentVO studentVO) {

    }
}
