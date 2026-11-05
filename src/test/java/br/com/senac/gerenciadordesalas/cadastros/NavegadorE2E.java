package br.com.senac.gerenciadordesalas.cadastros;

import org.junit.jupiter.api.Assumptions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Navegador Chrome sem interface gráfica para os testes E2E (Issue #15). Encapsula o Selenium para
 * que os testes leiam como um roteiro: abrir a tela, preencher, clicar e conferir o que o usuário vê.
 *
 * <p>No CI (variável {@code CI} definida pelo GitHub Actions) a falta do Chrome faz o teste falhar. Em
 * um computador local sem Chrome, o teste é ignorado em vez de falhar.
 */
final class NavegadorE2E {

    /** Tamanhos de janela usados nos testes de responsividade. */
    enum Tela {
        CELULAR(390, 844, true),
        TABLET(768, 1024, true),
        DESKTOP(1280, 800, false);

        final int largura;
        final int altura;
        final boolean movel;

        Tela(int largura, int altura, boolean movel) {
            this.largura = largura;
            this.altura = altura;
            this.movel = movel;
        }
    }

    private static final Duration ESPERA = Duration.ofSeconds(15);

    private final ChromeDriver driver;
    private final String base;
    private final WebDriverWait espera;

    private NavegadorE2E(ChromeDriver driver, String base) {
        this.driver = driver;
        this.base = base;
        this.espera = new WebDriverWait(driver, ESPERA);
    }

    static NavegadorE2E abrir(String base) {
        ChromeOptions opcoes = new ChromeOptions();
        opcoes.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
                "--window-size=1280,900", "--lang=pt-BR", "--remote-allow-origins=*");
        try {
            ChromeDriver driver = new ChromeDriver(opcoes);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            return new NavegadorE2E(driver, base);
        } catch (RuntimeException ex) {
            if (System.getenv("CI") != null) {
                throw ex;
            }
            return Assumptions.abort("Chrome indisponivel neste computador: " + ex.getMessage());
        }
    }

    void fechar() {
        driver.quit();
    }

    // ---------- navegação e leitura ----------

    void ir(String caminho) {
        driver.get(base + caminho);
    }

    String caminhoAtual() {
        return URI.create(driver.getCurrentUrl()).getPath();
    }

    String textoDaPagina() {
        return driver.findElement(By.tagName("body")).getText();
    }

    void esperarTexto(String texto) {
        espera.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), texto));
    }

    void esperarCaminho(String trecho) {
        espera.until(ExpectedConditions.urlContains(trecho));
    }

    /** Texto da célula {@code coluna} (a primeira é 1) da linha da tabela cuja primeira célula tem o texto dado. */
    String textoDaCelula(String textoDaLinha, int coluna) {
        return driver.findElement(By.xpath(
                "//tr[td[normalize-space()='" + textoDaLinha + "']]/td[" + coluna + "]")).getText();
    }

    // ---------- ações ----------

    void preencher(String id, String valor) {
        WebElement campo = driver.findElement(By.id(id));
        campo.clear();
        campo.sendKeys(valor);
    }

    /** Define o valor por script, o jeito confiável de preencher campos de data e hora. */
    void definirValor(String id, String valor) {
        WebElement campo = driver.findElement(By.id(id));
        executar("arguments[0].value = arguments[1];"
                + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));"
                + "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));", campo, valor);
    }

    void escolher(String id, String rotuloVisivel) {
        new Select(driver.findElement(By.id(id))).selectByVisibleText(rotuloVisivel);
    }

    void clicarLink(String texto) {
        driver.findElement(By.linkText(texto)).click();
    }

    void clicarBotao(String texto) {
        driver.findElement(By.xpath("//button[normalize-space()='" + texto + "']")).click();
    }

    void clicarNaLinha(String textoDaLinha, String link) {
        driver.findElement(By.xpath("//tr[td[normalize-space()='" + textoDaLinha
                + "']]//a[normalize-space()='" + link + "']")).click();
    }

    boolean existeLink(String texto) {
        return !driver.findElements(By.linkText(texto)).isEmpty();
    }

    boolean existeBotao(String texto) {
        return !driver.findElements(By.xpath("//button[normalize-space()='" + texto + "']")).isEmpty();
    }

    /** O navegador considera o campo válido (as regras de HTML como required, min e step)? */
    boolean campoValido(String id) {
        return Boolean.TRUE.equals(executar("return arguments[0].validity.valid;",
                driver.findElement(By.id(id))));
    }

    // ---------- sessão ----------

    void entrarComo(String email, String senha) {
        ir("/login");
        preencher("email", email);
        preencher("senha", senha);
        clicarBotao("Entrar");
        esperarCaminho("/professores");
    }

    void sair() {
        driver.manage().deleteAllCookies();
    }

    // ---------- responsividade ----------

    /** Emula o tamanho de janela, valendo para as próximas navegações. */
    void usarTela(Tela tela) {
        Map<String, Object> medidas = new HashMap<>();
        medidas.put("width", tela.largura);
        medidas.put("height", tela.altura);
        medidas.put("deviceScaleFactor", 1);
        medidas.put("mobile", tela.movel);
        driver.executeCdpCommand("Emulation.setDeviceMetricsOverride", medidas);
    }

    /** O Bootstrap, vindo da CDN, foi aplicado? A classe bg-light pinta o fundo de cinza claro. */
    boolean bootstrapCarregado() {
        return "rgb(248, 249, 250)".equals(
                executar("return getComputedStyle(document.body).backgroundColor;"));
    }

    long larguraDaJanela() {
        return numero("return window.innerWidth;");
    }

    long larguraDoConteudo() {
        return numero("return document.documentElement.scrollWidth;");
    }

    long larguraDoCampo(String id) {
        return Math.round(driver.findElement(By.id(id)).getRect().getWidth());
    }

    String rolagemHorizontalDe(String seletorCss) {
        return String.valueOf(executar(
                "return getComputedStyle(document.querySelector(arguments[0])).overflowX;", seletorCss));
    }

    boolean campoDentroDaJanela(String id) {
        return dentroDaJanela(driver.findElement(By.id(id)).getRect());
    }

    boolean botaoDentroDaJanela(String texto) {
        return dentroDaJanela(driver.findElement(
                By.xpath("//button[normalize-space()='" + texto + "']")).getRect());
    }

    private boolean dentroDaJanela(Rectangle retangulo) {
        return retangulo.getX() >= 0 && retangulo.getX() + retangulo.getWidth() <= larguraDaJanela();
    }

    private long numero(String script) {
        return ((Number) executar(script)).longValue();
    }

    private Object executar(String script, Object... argumentos) {
        return ((JavascriptExecutor) driver).executeScript(script, argumentos);
    }
}