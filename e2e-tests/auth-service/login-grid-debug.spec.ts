import { test, expect } from '@playwright/test';
import { defaultTestUser } from '../fixtures/auth';

test.describe('Login and Debug Blog Grid Layout', () => {
  test('should login, navigate to blog, and analyze grid layout', async ({ page }) => {
    // 1. 뷰포트 설정 및 접속
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('http://localhost:30000/');

    // 2. 로그인 수행
    // 'Login' 버튼 클릭
    await page.getByRole('button', { name: 'Login' }).click();
    
    // Auth0 로그인 폼이 나타날 때까지 대기
    await page.waitForSelector('div.auth0-lock-widget');
    console.log('Auth0 Lock widget is visible.');

    // 이메일과 비밀번호 입력
    await page.getByPlaceholder('yours@example.com').fill(defaultTestUser.email);
    await page.getByPlaceholder('your password').fill(defaultTestUser.password);

    // 'Sign In' 버튼 클릭
    await page.getByRole('button', { name: 'Sign In' }).click();
    console.log('Filled credentials and clicked Sign In.');

    // 3. 로그인 완료 후 블로그 페이지로 이동
    // 로그아웃 버튼이 보일 때까지 대기하여 로그인 완료 확인
    await page.waitForSelector('button:has-text("Logout")');
    console.log('Login successful, Logout button is visible.');

    // 'Blog' 네비게이션 링크 클릭
    await page.getByRole('link', { name: 'Blog' }).click();
    console.log('Clicked Blog navigation link.');

    // 4. 그리드 분석
    // 그리드 컨테이너가 나타날 때까지 대기
    const gridSelector = '.grid';
    await page.waitForSelector(gridSelector);
    await page.waitForTimeout(1000); // 렌더링 안정화
    console.log('Grid container is visible.');

    // 5. 주요 요소의 outerHTML과 실제 너비 측정
    const analysisData = await page.evaluate(() => {
      const portalShellMain = document.querySelector('main.flex-1');
      const blogPageInnerContainer = portalShellMain?.querySelector('.max-w-\[1920px\]');
      const gridContainer = blogPageInnerContainer?.querySelector('.grid');

      return {
        mainOuterHTML: portalShellMain?.outerHTML,
        dimensions: {
          windowInnerWidth: window.innerWidth,
          bodyClientWidth: document.body.clientWidth,
          portalShellMainClientWidth: portalShellMain?.clientWidth,
          blogPageInnerContainerClientWidth: blogPageInnerContainer?.clientWidth,
          gridContainerClientWidth: gridContainer?.clientWidth,
        },
        gridComputedStyle: gridContainer ? window.getComputedStyle(gridContainer).getPropertyValue('grid-template-columns') : null,
      };
    });

    // 6. 분석 데이터 출력
    console.log('\n--- レンダリングされたMain要素のHTML ---');
    console.log(analysisData.mainOuterHTML);
    console.log('-------------------------------------\n');
    
    console.log('--- 측정된 너비 (1920x1080) ---');
    console.log('Window Inner Width:', analysisData.dimensions.windowInnerWidth);
    console.log('Body Client Width:', analysisData.dimensions.bodyClientWidth);
    console.log('Portal Shell <main> Width:', analysisData.dimensions.portalShellMainClientWidth);
    console.log('Blog Page Inner Container Width:', analysisData.dimensions.blogPageInnerContainerClientWidth);
    console.log('Grid Container Width:', analysisData.dimensions.gridContainerClientWidth);
    console.log('-------------------------------------\n');

    console.log('--- 계산된 그리드 스타일 ---');
    console.log('Computed grid-template-columns:', analysisData.gridComputedStyle);
    const columnCount = analysisData.gridComputedStyle?.split(' ').length || 0;
    console.log(`Rendered with ${columnCount} columns.`);
    console.log('---------------------------\n');
    
    // 스크린샷 캡처
    await page.screenshot({ path: 'e2e-tests/test-results/login-grid-debug-screenshot.png', fullPage: true });

    // 최종 확인
    expect(columnCount, 'Column count should be greater than 2 on a 1920px screen.').toBeGreaterThan(2);
  });
});
