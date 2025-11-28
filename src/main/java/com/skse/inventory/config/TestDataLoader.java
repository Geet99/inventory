package com.skse.inventory.config;

import com.skse.inventory.model.*;
import com.skse.inventory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Component
@Profile("dev") // Only run in development mode
public class TestDataLoader implements CommandLineRunner {
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Autowired
    private ColorRepository colorRepository;
    
    @Autowired
    private VendorRepository vendorRepository;
    
    @Autowired
    private RateHeadRepository rateHeadRepository;
    
    @Autowired
    private PlanRepository planRepository;
    
    @Autowired
    private VendorMonthlyPaymentRepository vendorMonthlyPaymentRepository;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("Loading Test Data for Development...");
        System.out.println("=".repeat(60));
        
        // Create test data
        createColors();
        createRateHeads();
        createArticles();
        createVendors();
        createLastMonthPlans();
        createLastMonthVendorPayments();
        
        System.out.println("=".repeat(60));
        System.out.println("Test Data Loaded Successfully!");
        System.out.println("=".repeat(60));
    }
    
    private void createColors() {
        if (colorRepository.count() == 0) {
            System.out.println("Creating test colors...");
            String[] colors = {"Red", "Blue", "Black", "White", "Green", "Yellow"};
            for (String colorName : colors) {
                Color color = new Color();
                color.setName(colorName);
                colorRepository.save(color);
            }
            System.out.println("✓ Created " + colors.length + " colors");
        }
    }
    
    private void createRateHeads() {
        if (rateHeadRepository.count() == 0) {
            System.out.println("Creating test rate heads...");
            
            // Cutting rates
            RateHead cutting1 = new RateHead();
            cutting1.setName("Standard Cutting");
            cutting1.setOperationType(VendorRole.Cutting);
            cutting1.setCost(15.0);
            cutting1.setActive(true);
            rateHeadRepository.save(cutting1);
            
            // Printing rates
            RateHead printing1 = new RateHead();
            printing1.setName("Basic Printing");
            printing1.setOperationType(VendorRole.Printing);
            printing1.setCost(12.0);
            printing1.setActive(true);
            rateHeadRepository.save(printing1);
            
            RateHead printing2 = new RateHead();
            printing2.setName("Premium Printing");
            printing2.setOperationType(VendorRole.Printing);
            printing2.setCost(18.0);
            printing2.setActive(true);
            rateHeadRepository.save(printing2);
            
            // Stitching rates
            RateHead stitching1 = new RateHead();
            stitching1.setName("Standard Stitching");
            stitching1.setOperationType(VendorRole.Stitching);
            stitching1.setCost(20.0);
            stitching1.setActive(true);
            rateHeadRepository.save(stitching1);
            
            System.out.println("✓ Created 4 rate heads");
        }
    }
    
    private void createArticles() {
        if (articleRepository.count() == 0) {
            System.out.println("Creating test articles...");
            
            RateHead cuttingRate = rateHeadRepository.findByOperationType(VendorRole.Cutting).get(0);
            RateHead printingRate = rateHeadRepository.findByOperationType(VendorRole.Printing).get(0);
            RateHead stitchingRate = rateHeadRepository.findByOperationType(VendorRole.Stitching).get(0);
            
            Article article1 = new Article();
            article1.setName("Nike Classic");
            article1.setDescription("Classic Nike slipper design");
            article1.setSlipperCost(250.0);
            article1.setCuttingRateHead(cuttingRate);
            article1.setPrintingRateHead(printingRate);
            article1.setStitchingRateHead(stitchingRate);
            articleRepository.save(article1);
            
            Article article2 = new Article();
            article2.setName("Adidas Sport");
            article2.setDescription("Sports edition slipper");
            article2.setSlipperCost(300.0);
            article2.setCuttingRateHead(cuttingRate);
            article2.setPrintingRateHead(printingRate);
            article2.setStitchingRateHead(stitchingRate);
            articleRepository.save(article2);
            
            System.out.println("✓ Created 2 articles");
        }
    }
    
    private void createVendors() {
        if (vendorRepository.count() == 0) {
            System.out.println("Creating test vendors...");
            
            Vendor cuttingVendor = new Vendor();
            cuttingVendor.setName("Cutting Master");
            cuttingVendor.setRole(VendorRole.Cutting);
            cuttingVendor.setActive(true);
            cuttingVendor.setPaymentDue(0.0);
            vendorRepository.save(cuttingVendor);
            
            Vendor printingVendor = new Vendor();
            printingVendor.setName("Print Pro");
            printingVendor.setRole(VendorRole.Printing);
            printingVendor.setActive(true);
            printingVendor.setPaymentDue(0.0);
            vendorRepository.save(printingVendor);
            
            Vendor stitchingVendor = new Vendor();
            stitchingVendor.setName("Stitch Expert");
            stitchingVendor.setRole(VendorRole.Stitching);
            stitchingVendor.setActive(true);
            stitchingVendor.setPaymentDue(0.0);
            vendorRepository.save(stitchingVendor);
            
            System.out.println("✓ Created 3 vendors");
        }
    }
    
    private void createLastMonthPlans() {
        System.out.println("Creating last month's completed plans...");
        
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        Article article = articleRepository.findByName("Nike Classic").orElse(null);
        
        // Find color by name
        Color color = colorRepository.findAll().stream()
            .filter(c -> c.getName().equals("Red"))
            .findFirst()
            .orElse(null);
        
        Vendor cuttingVendor = vendorRepository.findByRole(VendorRole.Cutting).get(0);
        Vendor printingVendor = vendorRepository.findByRole(VendorRole.Printing).get(0);
        Vendor stitchingVendor = vendorRepository.findByRole(VendorRole.Stitching).get(0);
        RateHead printingRate = rateHeadRepository.findByOperationType(VendorRole.Printing).get(0);
        
        if (article != null && color != null) {
            // Create 3 completed plans from last month
            for (int i = 1; i <= 3; i++) {
                Plan plan = new Plan();
                plan.setPlanNumber("PLAN-LM-" + i);
                plan.setArticleName(article.getName());
                plan.setColor(color.getName());
                plan.setSizeQuantityPairs("6:50, 7:40, 8:30");
                plan.setTotal(120);
                plan.setStatus(PlanStatus.Completed);
                plan.setCreateDate(lastMonth.withDayOfMonth(5));
                
                plan.setCuttingVendor(cuttingVendor);
                plan.setCuttingStartDate(lastMonth.withDayOfMonth(6));
                plan.setCuttingEndDate(lastMonth.withDayOfMonth(8));
                plan.setCuttingVendorPaymentDue(article.getCuttingCost() * 120);
                
                plan.setPrintingVendor(printingVendor);
                plan.setPrintingRateHead(printingRate);
                plan.setPrintingStartDate(lastMonth.withDayOfMonth(9));
                plan.setPrintingEndDate(lastMonth.withDayOfMonth(11));
                plan.setPrintingVendorPaymentDue(printingRate.getCost() * 120);
                
                plan.setStitchingVendor(stitchingVendor);
                plan.setStitchingStartDate(lastMonth.withDayOfMonth(12));
                plan.setStitchingEndDate(lastMonth.withDayOfMonth(15));
                plan.setStitchingVendorPaymentDue(article.getStitchingCost() * 120);
                
                planRepository.save(plan);
            }
            System.out.println("✓ Created 3 completed plans from last month");
        }
    }
    
    private void createLastMonthVendorPayments() {
        System.out.println("Creating last month's vendor payment records...");
        
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        String lastMonthYear = VendorMonthlyPayment.getPreviousMonthYear();
        
        Vendor cuttingVendor = vendorRepository.findByRole(VendorRole.Cutting).get(0);
        Vendor printingVendor = vendorRepository.findByRole(VendorRole.Printing).get(0);
        Vendor stitchingVendor = vendorRepository.findByRole(VendorRole.Stitching).get(0);
        
        // Cutting vendor - Pending payment (not paid yet)
        VendorMonthlyPayment cuttingPayment = new VendorMonthlyPayment();
        cuttingPayment.setVendor(cuttingVendor);
        cuttingPayment.setMonthYear(lastMonthYear);
        cuttingPayment.setOperationType(VendorRole.Cutting);
        cuttingPayment.setTotalDue(5400.0); // 3 plans * 120 units * 15 per unit
        cuttingPayment.setPaidAmount(0.0);
        cuttingPayment.setStatus(PaymentStatus.PENDING);
        cuttingPayment.setCreatedDate(lastMonth.atDay(15));
        cuttingPayment.setLastUpdatedDate(lastMonth.atDay(15));
        vendorMonthlyPaymentRepository.save(cuttingPayment);
        
        // Printing vendor - Partially paid
        VendorMonthlyPayment printingPayment = new VendorMonthlyPayment();
        printingPayment.setVendor(printingVendor);
        printingPayment.setMonthYear(lastMonthYear);
        printingPayment.setOperationType(VendorRole.Printing);
        printingPayment.setTotalDue(4320.0); // 3 plans * 120 units * 12 per unit
        printingPayment.setPaidAmount(2000.0);
        printingPayment.setStatus(PaymentStatus.PARTIAL);
        printingPayment.setCreatedDate(lastMonth.atDay(15));
        printingPayment.setLastUpdatedDate(lastMonth.atDay(20));
        vendorMonthlyPaymentRepository.save(printingPayment);
        
        // Stitching vendor - Fully paid (to test cleanup)
        VendorMonthlyPayment stitchingPayment = new VendorMonthlyPayment();
        stitchingPayment.setVendor(stitchingVendor);
        stitchingPayment.setMonthYear(lastMonthYear);
        stitchingPayment.setOperationType(VendorRole.Stitching);
        stitchingPayment.setTotalDue(7200.0); // 3 plans * 120 units * 20 per unit
        stitchingPayment.setPaidAmount(7200.0);
        stitchingPayment.setStatus(PaymentStatus.PAID);
        stitchingPayment.setCreatedDate(lastMonth.atDay(15));
        stitchingPayment.setLastUpdatedDate(lastMonth.atDay(25));
        vendorMonthlyPaymentRepository.save(stitchingPayment);
        
        System.out.println("✓ Created 3 vendor monthly payment records:");
        System.out.println("  - Cutting: ₹5,400 (PENDING)");
        System.out.println("  - Printing: ₹4,320 (PARTIAL - ₹2,000 paid, ₹2,320 remaining)");
        System.out.println("  - Stitching: ₹7,200 (PAID - ready for cleanup)");
    }
}

