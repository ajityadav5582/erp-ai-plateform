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
        BigDecimal price = item.getSellingPrice();
        if (price == null) {
            throw new ItemNotPricedException(
                    "Item '" + item.getName() + "' has no selling price and cannot be sold at POS");
        }
        PosVatType vatType = toVatType(item.getTaxabilityType());
        BigDecimal vatRate = resolveVatRate(vatType, item.getVatRate());
        return new ResolvedPrice(price.setScale(MONEY_SCALE, RoundingMode.HALF_UP), vatType, vatRate);
    }

    /**
     * Prices one cart line for an already-fetched item, using the domain to
     * compute the net line amount and VAT.
     */
    public PricedLine priceLine(ItemService.ItemResponse item, BigDecimal quantity, BigDecimal discountPercentage) {
        ResolvedPrice price = resolvePrice(item);
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
        List<PricedLine> lines = requests.stream()
                .map(request -> {
                    ItemService.ItemResponse item = itemService.findById(request.itemId())
                            .orElseThrow(() -> new ItemNotPricedException(
                                    "Item not found with ID: " + request.itemId()));
                    return priceLine(item, request.quantity(), request.discountPercentage());
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
