package TestNg;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import java.time.Duration;

public class SaucedemoTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private final String BASE_URL = "https://www.saucedemo.com/v1/index.html";
    private final String VALID_PASSWORD = "secret_sauce";

    @BeforeMethod
    public void setup() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get(BASE_URL);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterMethod
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void login(String username, String password) {
        WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("user-name")));
        usernameField.clear();
        usernameField.sendKeys(username);

        WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        passwordField.clear();
        passwordField.sendKeys(password);

        wait.until(ExpectedConditions.elementToBeClickable(By.id("login-button"))).click();
    }

    private void logout() {
        try {
            // Check if menu is already open by looking for the menu wrap visibility
            boolean isMenuOpen = driver.findElements(By.cssSelector(".bm-menu-wrap[style*='translateX(0px)']")).size() > 0;

            if (!isMenuOpen) {
                WebElement hamburgerButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".bm-burger-button")));
                // Use JavaScript click as a fallback to avoid interception
                try {
                    hamburgerButton.click();
                } catch (ElementClickInterceptedException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", hamburgerButton);
                }
            }

            WebElement logoutLink = wait.until(ExpectedConditions.elementToBeClickable(By.id("logout_sidebar_link")));
            logoutLink.click();

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("user-name")));
        } catch (TimeoutException e) {
            System.out.println("Logout skipped: User might not be logged in or menu not accessible.");
        }
    }

    private void validateInventoryPage() {
        WebElement inventoryContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("inventory_container")));
        Assert.assertTrue(inventoryContainer.isDisplayed(), "Inventory page failed to load.");
    }

    @Test(priority = 1)
    public void testStandardUserLogin() {
        login("standard_user", VALID_PASSWORD);
        Assert.assertTrue(driver.getCurrentUrl().contains("/v1/inventory.html"), "Login failed for standard_user");
        validateInventoryPage();
        logout();
    }

    @Test(priority = 2)
    public void testLockedOutUserLogin() {
        login("locked_out_user", VALID_PASSWORD);
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='error']")));
        Assert.assertTrue(errorMsg.getText().contains("locked out"), "Expected locked out message not found.");
    }

    @Test(priority = 3)
    public void testProblemUserLogin() {
        login("problem_user", VALID_PASSWORD);
        Assert.assertTrue(driver.getCurrentUrl().contains("/v1/inventory.html"), "Login failed for problem_user");
        logout();
    }

    @Test(priority = 4)
    public void testPerformanceGlitchUserLogin() {
        login("performance_glitch_user", VALID_PASSWORD);
        Assert.assertTrue(driver.getCurrentUrl().contains("/v1/inventory.html"), "Login failed for performance_glitch_user");
        logout();
    }

    @Test(priority = 5)
    public void testValidUsernameInvalidPassword() {
        login("standard_user", "wrong_password");
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='error']")));
        Assert.assertTrue(errorMsg.getText().contains("Username and password do not match"),
                "Expected invalid login message not found.");
    }

    @Test(priority = 6)
    public void testInvalidUsernameValidPassword() {
        login("invalid_user", VALID_PASSWORD);
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='error']")));
        Assert.assertTrue(errorMsg.getText().contains("Username and password do not match"),
                "Expected invalid login message not found.");
    }

    @Test(priority = 7)
    public void testHamburgerMenu() {
        login("standard_user", VALID_PASSWORD);
        WebElement hamburgerButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".bm-burger-button")));
        try {
            hamburgerButton.click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", hamburgerButton);
        }

        WebElement logoutLink = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("logout_sidebar_link")));
        Assert.assertTrue(logoutLink.isDisplayed(), "Logout link not visible in hamburger menu.");
        // Skip logout here to avoid redundant menu click; logout is tested elsewhere
    }

    @Test(priority = 8)
    public void testProductImageClick() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // Locate the product image on the inventory page
        WebElement productImage = wait.until(ExpectedConditions.elementToBeClickable(By.className("inventory_item_img")));
        Assert.assertTrue(productImage.isDisplayed(), "Product image is not visible.");
        productImage.click();

        // Validate the product name on the details page
        WebElement productName = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("inventory_details_name")));
        Assert.assertTrue(productName.getText().contains("Sauce Labs Backpack"), "Product name does not match.");

        logout();
    }

    @Test(priority = 9)
    public void testAddToCartButtonChangesToRemove() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // Locate the "Add to Cart" button for Sauce Labs Backpack using a simpler locator
        WebElement cartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'ADD TO CART')]")
        ));

        // Click the button and wait for it to change to "REMOVE"
        cartButton.click();
        wait.until(ExpectedConditions.textToBePresentInElement(cartButton, "REMOVE"));

        // Verify the button now says "REMOVE"
        Assert.assertEquals(cartButton.getText(), "REMOVE", "Button text did not change to REMOVE.");

        logout();
    }

    @Test(priority = 10)
    public void testShoppingCartContainer() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // Add an item to the cart (Sauce Labs Backpack)
        WebElement cartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'ADD TO CART')]")
        ));
        cartButton.click();

        // Locate the shopping cart badge
        WebElement cartBadge = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className("shopping_cart_badge")
        ));

        // Verify the badge count is 1
        Assert.assertEquals(cartBadge.getText(), "1", "Shopping cart badge count is incorrect.");

        logout();
    }

    @Test(priority = 11)
    public void testContinueShoppingButton() throws InterruptedException {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement cartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'ADD TO CART')]")
        ));
        cartButton.click();

        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        cartLink.click();

        WebElement continueShoppingButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[text()='Continue Shopping']")
        ));

        // Scroll and JS click
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView(true);", continueShoppingButton);
        js.executeScript("arguments[0].click();", continueShoppingButton);

        // Wait to allow navigation
        Thread.sleep(2000); // you can replace with a wait for URL to change

        System.out.println("Current URL after clicking Continue Shopping: " + driver.getCurrentUrl());
        Assert.assertTrue(driver.getCurrentUrl().contains("inventory.html"), "Did not navigate to inventory page.");

        logout();
    }


    // Test 12: Add another item to cart after continuing shopping
    @Test(priority = 12)
    public void testAddAnotherItemAfterContinuingShopping() throws InterruptedException {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        // Simulate the state where one item is already in the cart (Sauce Labs Backpack)
        WebElement initialCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Backpack')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", initialCartButton); // Use JS click for reliability

        // Navigate to the cart
        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        cartLink.click();

        // Click "Continue Shopping" to return to inventory page
        WebElement continueShoppingButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[text()='Continue Shopping']")
        ));
        js.executeScript("arguments[0].scrollIntoView(true);", continueShoppingButton);
        js.executeScript("arguments[0].click();", continueShoppingButton);

        // Wait for navigation back to inventory page
        wait.until(ExpectedConditions.urlContains("inventory.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("inventory.html"), "Did not navigate back to inventory page.");
        System.out.println("Current URL: " + driver.getCurrentUrl());

        // Add another item to the cart (Sauce Labs Bolt T-Shirt)
        try {
            WebElement secondCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Bolt T-Shirt')]//following::button[contains(text(), 'ADD TO CART')]")
            ));
            System.out.println("Second item 'Add to Cart' button found.");
            js.executeScript("arguments[0].click();", secondCartButton); // Use JS click for reliability
        } catch (TimeoutException e) {
            System.out.println("Second item 'Add to Cart' button not found. Printing page source for debugging:");
            System.out.println(driver.getPageSource());
            throw e;
        }

        // Verify the cart badge count is now 2
        WebElement cartBadge = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className("shopping_cart_badge")
        ));
        Assert.assertEquals(cartBadge.getText(), "2", "Shopping cart badge count should be 2 after adding another item.");

        logout();
    }

    @Test(priority = 13)
    public void testProceedToCheckoutWithEmptyForm() throws InterruptedException {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Add first item (Sauce Labs Backpack)
        WebElement firstCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Backpack')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", firstCartButton);

        // Add second item (Sauce Labs Bike Light)
        WebElement secondCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Bike Light')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", secondCartButton);

        // Navigate to the cart
        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        js.executeScript("arguments[0].click();", cartLink);

        // Verify cart page and badge count
        wait.until(ExpectedConditions.urlContains("cart.html"));
        WebElement cartBadge = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className("shopping_cart_badge")
        ));
        Assert.assertEquals(cartBadge.getText(), "2", "Shopping cart badge count should be 2 before checkout.");

        // Click "Checkout" button
        WebElement checkoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("checkout_button")
        ));
        js.executeScript("arguments[0].click();", checkoutButton);

        // Verify navigation to checkout page
        wait.until(ExpectedConditions.urlContains("checkout-step-one.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-one.html"), "Did not navigate to checkout page.");

        // Submit form with all fields empty
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.btn_primary.cart_button")
        ));
        js.executeScript("arguments[0].click();", continueButton);

        // Validate error message
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("h3[data-test='error']")
        ));
        Assert.assertTrue(errorMsg.getText().contains("First Name is required"), "Expected error message for empty form fields not found.");

        logout();
    }



    @Test(priority = 14)
    public void testCheckoutFormFirstNameEmpty() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Add first item (Sauce Labs Backpack)
        WebElement firstCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Backpack')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", firstCartButton);

        // Add second item (Sauce Labs Bike Light)
        WebElement secondCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Bike Light')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", secondCartButton);

        // Navigate to the cart
        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        js.executeScript("arguments[0].click();", cartLink);

        // Verify cart page
        wait.until(ExpectedConditions.urlContains("cart.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("cart.html"), "Did not land on cart page");

        // Click "Checkout" button
        WebElement checkoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("checkout_button")
        ));
        js.executeScript("arguments[0].click();", checkoutButton);

        // Verify navigation to checkout page
        wait.until(ExpectedConditions.urlContains("checkout-step-one.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-one.html"), "Did not navigate to checkout page.");

        // Fill form with First Name empty, but other fields filled
        driver.findElement(By.id("last-name")).sendKeys("Doe");
        driver.findElement(By.id("postal-code")).sendKeys("12345");

        // Submit form
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.btn_primary.cart_button")
        ));
        js.executeScript("arguments[0].click();", continueButton);

        // Validate error message
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("h3[data-test='error']")
        ));
        Assert.assertTrue(errorMsg.getText().contains("First Name is required"), "Expected error message for missing First Name not found.");

        logout();
    }

    @Test(priority = 15)
    public void testCheckoutFormLastNameEmpty() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Add first item (Sauce Labs Backpack)
        WebElement firstCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Backpack')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", firstCartButton);

        // Add second item (Sauce Labs Bike Light)
        WebElement secondCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Bike Light')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", secondCartButton);

        // Navigate to the cart
        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        js.executeScript("arguments[0].click();", cartLink);

        // Verify cart page
        wait.until(ExpectedConditions.urlContains("cart.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("cart.html"), "Did not land on cart page");

        // Click "Checkout" button
        WebElement checkoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("checkout_button")
        ));
        js.executeScript("arguments[0].click();", checkoutButton);

        // Verify navigation to checkout page
        wait.until(ExpectedConditions.urlContains("checkout-step-one.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-one.html"), "Did not navigate to checkout page.");

        // Fill form with Last Name empty, but other fields filled
        WebElement firstNameField = driver.findElement(By.id("first-name"));
        firstNameField.clear(); // Ensure the field is cleared
        firstNameField.sendKeys("John");
        // Verify the first name field value
        String firstNameValue = firstNameField.getAttribute("value");
        System.out.println("First Name field value: " + firstNameValue);
        Assert.assertEquals(firstNameValue, "John", "First Name field was not set correctly.");

        WebElement lastNameField = driver.findElement(By.id("last-name"));
        lastNameField.clear(); // Explicitly clear Last Name
        lastNameField.sendKeys(""); // Ensure Last Name is empty

        driver.findElement(By.id("postal-code")).sendKeys("12345");

        // Submit form
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.btn_primary.cart_button")
        ));
        js.executeScript("arguments[0].click();", continueButton);

        // Validate error message
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("h3[data-test='error']")
        ));
        String actualErrorMessage = errorMsg.getText();
        System.out.println("Actual error message: " + actualErrorMessage); // Debug log
        Assert.assertTrue(actualErrorMessage.toLowerCase().contains("last name is required"),
                "Expected error message for missing Last Name not found. Actual: " + actualErrorMessage);

        logout();
    }

    @Test(priority = 16)
    public void testCheckoutFormPostalCodeEmpty() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Add first item (Sauce Labs Backpack)
        WebElement firstCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Backpack')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", firstCartButton);

        // Add second item (Sauce Labs Bike Light)
        WebElement secondCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Bike Light')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", secondCartButton);

        // Navigate to the cart
        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        js.executeScript("arguments[0].click();", cartLink);

        // Verify cart page
        wait.until(ExpectedConditions.urlContains("cart.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("cart.html"), "Did not land on cart page");

        // Click "Checkout" button
        WebElement checkoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("checkout_button")
        ));
        js.executeScript("arguments[0].click();", checkoutButton);

        // Verify navigation to checkout page
        wait.until(ExpectedConditions.urlContains("checkout-step-one.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-one.html"), "Did not navigate to checkout page.");

        // Fill form with Postal Code empty, but other fields filled
        driver.findElement(By.id("first-name")).sendKeys("John");
        driver.findElement(By.id("last-name")).sendKeys("Doe");

        // Submit form
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.btn_primary.cart_button")
        ));
        js.executeScript("arguments[0].click();", continueButton);

        // Validate error message
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("h3[data-test='error']")
        ));
        Assert.assertTrue(errorMsg.getText().contains("Postal Code is required"), "Expected error message for missing Postal Code not found.");

        logout();
    }

    @Test(priority = 17)
    public void testCheckoutFormWithAllDetails() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Add first item (Sauce Labs Backpack)
        WebElement firstCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Backpack')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", firstCartButton);

        // Add second item (Sauce Labs Bike Light)
        WebElement secondCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Bike Light')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", secondCartButton);

        // Navigate to the cart
        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        js.executeScript("arguments[0].click();", cartLink);

        // Verify cart page
        wait.until(ExpectedConditions.urlContains("cart.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("cart.html"), "Did not land on cart page");

        // Click "Checkout" button
        WebElement checkoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("checkout_button")
        ));
        js.executeScript("arguments[0].click();", checkoutButton);

        // Verify navigation to checkout page
        wait.until(ExpectedConditions.urlContains("checkout-step-one.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-one.html"), "Did not navigate to checkout page.");

        // Fill form with all details
        driver.findElement(By.id("first-name")).sendKeys("John");
        driver.findElement(By.id("last-name")).sendKeys("Doe");
        driver.findElement(By.id("postal-code")).sendKeys("12345");

        // Submit form
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.btn_primary.cart_button")
        ));
        js.executeScript("arguments[0].click();", continueButton);

        // Verify navigation to checkout step two (overview page)
        wait.until(ExpectedConditions.urlContains("checkout-step-two.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-two.html"), "Did not navigate to checkout step two page.");

        // Optionally, validate that the cart items are displayed on the overview page
        WebElement cartList = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className("cart_list")
        ));
        java.util.List<WebElement> cartItems = cartList.findElements(By.className("cart_item"));
        Assert.assertEquals(cartItems.size(), 2, "Expected 2 items in the cart on the checkout overview page.");

        logout();
    }

    @Test(priority = 18)
    public void testVerifyItemPricesAndTotal() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Add first item (Sauce Labs Backpack)
        WebElement firstCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Backpack')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", firstCartButton);

        // Add second item (Sauce Labs Bike Light)
        WebElement secondCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Bike Light')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", secondCartButton);

        // Navigate to the cart
        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        js.executeScript("arguments[0].click();", cartLink);

        // Verify cart page
        wait.until(ExpectedConditions.urlContains("cart.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("cart.html"), "Did not land on cart page");

        // Click "Checkout" button
        WebElement checkoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("checkout_button")
        ));
        js.executeScript("arguments[0].click();", checkoutButton);

        // Verify navigation to checkout page
        wait.until(ExpectedConditions.urlContains("checkout-step-one.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-one.html"), "Did not navigate to checkout page.");

        // Fill form with all details
        driver.findElement(By.id("first-name")).sendKeys("John");
        driver.findElement(By.id("last-name")).sendKeys("Doe");
        driver.findElement(By.id("postal-code")).sendKeys("12345");

        // Submit form
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.btn_primary.cart_button")
        ));
        js.executeScript("arguments[0].click();", continueButton);

        // Verify navigation to checkout step two (overview page)
        wait.until(ExpectedConditions.urlContains("checkout-step-two.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-two.html"), "Did not navigate to checkout step two page.");

        // Extract item prices
        java.util.List<WebElement> itemPrices = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector(".cart_item .inventory_item_price")
        ));
        Assert.assertEquals(itemPrices.size(), 2, "Expected 2 items in the cart on the checkout overview page.");

        // Calculate sum of item prices
        double calculatedTotal = 0.0;
        for (WebElement priceElement : itemPrices) {
            String priceText = priceElement.getText().replace("$", ""); // Remove the '$' sign
            double price = Double.parseDouble(priceText);
            calculatedTotal += price;
        }
        System.out.println("Calculated total of item prices: $" + calculatedTotal);

        // Extract the displayed "Item total"
        WebElement subtotalElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".summary_subtotal_label")
        ));
        String subtotalText = subtotalElement.getText(); // e.g., "Item total: $39.98"
        String subtotalValue = subtotalText.split("\\$")[1]; // Extract the number after the '$'
        double displayedTotal = Double.parseDouble(subtotalValue);
        System.out.println("Displayed item total: $" + displayedTotal);

        // Compare calculated total with displayed total
        Assert.assertEquals(calculatedTotal, displayedTotal, 0.01,
                "Calculated total does not match the displayed item total. Calculated: $" + calculatedTotal + ", Displayed: $" + displayedTotal);

        logout();
    }

    @Test(priority = 19)
    public void testCompleteCheckoutAndVerify() {
        login("standard_user", VALID_PASSWORD);
        validateInventoryPage();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Add first item (Sauce Labs Backpack)
        WebElement firstCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Backpack')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", firstCartButton);

        // Add second item (Sauce Labs Bike Light)
        WebElement secondCartButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@class='inventory_item_label'][contains(., 'Sauce Labs Bike Light')]//following::button[contains(text(), 'ADD TO CART')]")
        ));
        js.executeScript("arguments[0].click();", secondCartButton);

        // Navigate to the cart
        WebElement cartLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("shopping_cart_link")
        ));
        js.executeScript("arguments[0].click();", cartLink);

        // Verify cart page
        wait.until(ExpectedConditions.urlContains("cart.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("cart.html"), "Did not land on cart page");

        // Click "Checkout" button
        WebElement checkoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("checkout_button")
        ));
        js.executeScript("arguments[0].click();", checkoutButton);

        // Verify navigation to checkout page
        wait.until(ExpectedConditions.urlContains("checkout-step-one.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-one.html"), "Did not navigate to checkout page.");

        // Fill form with all details
        driver.findElement(By.id("first-name")).sendKeys("John");
        driver.findElement(By.id("last-name")).sendKeys("Doe");
        driver.findElement(By.id("postal-code")).sendKeys("12345");

        // Submit form
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.btn_primary.cart_button")
        ));
        js.executeScript("arguments[0].click();", continueButton);

        // Verify navigation to checkout step two (overview page)
        wait.until(ExpectedConditions.urlContains("checkout-step-two.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-step-two.html"), "Did not navigate to checkout step two page.");

        // Validate Payment Information
        java.util.List<WebElement> summaryValues = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector(".summary_value_label")
        ));
        Assert.assertEquals(summaryValues.get(0).getText(), "SauceCard #31337", "Payment Information does not match expected value.");

        // Validate Shipping Information
        Assert.assertEquals(summaryValues.get(1).getText(), "FREE PONY EXPRESS DELIVERY!", "Shipping Information does not match expected value.");

        // Extract and log Item total (subtotal without tax)
        WebElement subtotalElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".summary_subtotal_label")
        ));
        String subtotalText = subtotalElement.getText(); // e.g., "Item total: $39.98"
        double itemTotal = Double.parseDouble(subtotalText.split("\\$")[1]);
        System.out.println("Item total (without tax): $" + itemTotal);

        // Extract and log Tax
        WebElement taxElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".summary_tax_label")
        ));
        String taxText = taxElement.getText(); // e.g., "Tax: $3.20"
        double tax = Double.parseDouble(taxText.split("\\$")[1]);
        System.out.println("Tax: $" + tax);

        // Extract and log Total (with tax)
        WebElement totalElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".summary_total_label")
        ));
        String totalText = totalElement.getText(); // e.g., "Total: $43.18"
        double totalWithTax = Double.parseDouble(totalText.split("\\$")[1]);
        System.out.println("Total (with tax): $" + totalWithTax);

        // Verify that Total = Item total + Tax
        double calculatedTotalWithTax = itemTotal + tax;
        System.out.println("Calculated total (item total + tax): $" + calculatedTotalWithTax);
        Assert.assertEquals(calculatedTotalWithTax, totalWithTax, 0.01,
                "Calculated total with tax does not match the displayed total. Calculated: $" + calculatedTotalWithTax + ", Displayed: $" + totalWithTax);

        // Click "Finish" button
        WebElement finishButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a.btn_action.cart_button")
        ));
        js.executeScript("arguments[0].click();", finishButton);

        // Verify navigation to order confirmation page
        wait.until(ExpectedConditions.urlContains("checkout-complete.html"));
        Assert.assertTrue(driver.getCurrentUrl().contains("checkout-complete.html"), "Did not navigate to order confirmation page.");

        // Verify confirmation message
        WebElement confirmationMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("h2.complete-header")
        ));
        Assert.assertEquals(confirmationMessage.getText(), "THANK YOU FOR YOUR ORDER", "Order confirmation message not found.");

        logout();
    }
}