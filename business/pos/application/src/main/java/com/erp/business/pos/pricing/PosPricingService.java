package com.erp.business.pos.pricing;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.pos.domain.PosTransactionLine;
import com.erp.business.pos.domain.PosVatType;
import com.erp.business.pos.pricing.exception.ItemNotPricedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The single pricing authority for POS.
 *
 * <p>Resolves an item's selling price and VAT treatment from the Inventory
 * catalogue and delegates every money calculation to the domain
 * ({@link PosTransactionLine}), so a preview can never disagree with what
 * checkout persists. Prices are always resolved server-side; a caller never
 * supplies a unit price.
 */
@Service
@Transactional(readOnly = true)
public class PosPricingService {

    private static final int MONEY_SCALE = 4;

    private final ItemService itemService;

    public PosPricingService(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Resolves the selling price and VAT treatment for an already-fetched item.
     *
     * @throws ItemNotPricedException if the item carries no selling price
     */
    public ResolvedPrice resolvePrice(ItemService.ItemResponse item) {
        return resolvePrice(item, true);
    }

    /**
     * Resolves the selling price and VAT treatment for an already-fetched item.
     *
     * @param vatApplicable when {@code false} (PAN-only / non-VAT-registered
     *                     company) every line is forced to {@link PosVatType#EXEMPT}
     *                     with a zero VAT rate, per Nepal IRD cash-bill rules.
     * @throws ItemNotPricedException if the item carries no selling price
     */
    public ResolvedPrice resolvePrice(ItemService.ItemResponse item, boolean vatApplicable) {
        BigDecimal price = item.getSellingPrice();
        if (price == null) {
            throw new ItemNotPricedException(
                    "Item '" + item.getName() + "' has no selling price and cannot be sold at POS");
        }
        PosVatType vatType = toVatType(item.getTaxabilityType());
        if (!vatApplicable) {
            vatType = PosVatType.EXEMPT;
        }
        BigDecimal vatRate = resolveVatRate(vatType, item.getVatRate());

        // VAT-inclusive pricing: when the catalogue price already contains VAT,
        // convert it to the VAT-exclusive unit price so the line amount and VAT
        // are not double-counted.
        BigDecimal unitPrice = price;
        if (Boolean.TRUE.equals(item.getIsVatInclusive()) && vatType == PosVatType.TAXABLE
                && vatRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisor = BigDecimal.ONE.add(vatRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
            unitPrice = price.divide(divisor, MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return new ResolvedPrice(unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP), vatType, vatRate);
    }

    /**
     * Prices one cart line for an already-fetched item, using the domain to
     * compute the net line amount and VAT.
     */
    public PricedLine priceLine(ItemService.ItemResponse item, BigDecimal quantity, BigDecimal discountPercentage) {
        return priceLine(item, quantity, discountPercentage, true);
    }

    public PricedLine priceLine(ItemService.ItemResponse item, BigDecimal quantity, BigDecimal discountPercentage,
                               boolean vatApplicable) {
        ResolvedPrice price = resolvePrice(item, vatApplicable);
        PosTransactionLine line = PosTransactionLine.of(
                1, item.getId(), item.getSku(), item.getName(), item.getBarcode(), item.getUnitCode(),
                quantity, price.unitPrice(), discountPercentage, price.vatType(), price.vatRate(), null);

        return new PricedLine(
                line.getItemId(), line.getItemCode(), line.getItemName(), line.getUomCode(),
                line.getQuantity(), line.getUnitPrice(), line.getDiscountPercentage(),
                line.getVatType(), line.getVatRate(), line.getLineAmount(), line.vatAmount());
    }

    /**
     * Prices a whole cart and rolls up the header totals. Each item is fetched
     * once from the catalogue.
     */
    public PricePreview preview(List<PriceLineRequest> requests) {
        return preview(requests, true);
    }

    public PricePreview preview(List<PriceLineRequest> requests, boolean vatApplicable) {
        List<PricedLine> lines = requests.stream()
                .map(request -> {
                    ItemService.ItemResponse item = itemService.findById(request.itemId())
                            .orElseThrow(() -> new ItemNotPricedException(
                                    "Item not found with ID: " + request.itemId()));
                    return priceLine(item, request.quantity(), request.discountPercentage(), vatApplicable);
                })
                .toList();

        BigDecimal subtotal = lines.stream()
                .map(PricedLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal vatTotal = lines.stream()
                .map(PricedLine::vatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(vatTotal).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new PricePreview(lines, subtotal, vatTotal, total);
    }

    private static BigDecimal resolveVatRate(PosVatType vatType, BigDecimal vatRate) {
        if (vatType != PosVatType.TAXABLE) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return (vatRate != null ? vatRate : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private static PosVatType toVatType(String taxability) {
        if (taxability == null) {
            return PosVatType.TAXABLE;
        }
        return switch (taxability.toUpperCase()) {
            case "EXEMPT" -> PosVatType.EXEMPT;
            case "ZERO_RATED" -> PosVatType.ZERO_RATED;
            case "EXPORT" -> PosVatType.EXPORT;
            default -> PosVatType.TAXABLE;
        };
    }
}
