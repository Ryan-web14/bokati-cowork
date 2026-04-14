package com.sni.bokaticowork.features.inventory.importer.service;

import com.sni.bokaticowork.features.inventory.asset.service.interfaces.AssetService;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemService;
import com.sni.bokaticowork.features.inventory.importer.enums.InventoryImportType;
import com.sni.bokaticowork.features.inventory.procurement.service.ProcurementService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryImportServiceImplTest {

    @Mock
    private InventoryItemService itemService;
    @Mock
    private StockService stockService;
    @Mock
    private AssetService assetService;
    @Mock
    private ProcurementService procurementService;

    private final TabularInventoryFileReader fileReader = new TabularInventoryFileReader();

    @InjectMocks
    private InventoryImportServiceImpl service;

    @Test
    void shouldValidateCsvItemsInDryRunWithoutCallingService() {
        service = new InventoryImportServiceImpl(fileReader, itemService, stockService, assetService, procurementService);
        MockMultipartFile file = csv("items.csv", """
                item_code,name,item_type,tracking_type,default_cost,sale_price
                ITM-001,Chaise,ASSET,SERIAL,10000,15000
                """);

        var response = service.importFile(InventoryImportType.ITEMS, file, true);

        assertTrue(response.isDryRun());
        assertEquals(1, response.getSuccessCount());
        assertEquals(0, response.getErrorCount());
        verify(itemService, never()).create(any());
    }

    @Test
    void shouldReportInvalidLineWithoutStoppingImport() {
        service = new InventoryImportServiceImpl(fileReader, itemService, stockService, assetService, procurementService);
        MockMultipartFile file = csv("items.csv", """
                item_code,name,item_type
                ITM-001,,ASSET
                ITM-002,Table,ASSET
                """);

        var response = service.importFile(InventoryImportType.ITEMS, file, true);

        assertEquals(2, response.getTotalRows());
        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getErrorCount());
        assertFalse(response.getRows().getFirst().isSuccess());
    }

    @Test
    void shouldReturnDuplicateErrorFromBusinessService() {
        service = new InventoryImportServiceImpl(fileReader, itemService, stockService, assetService, procurementService);
        MockMultipartFile file = csv("items.csv", """
                item_code,name,item_type
                ITM-001,Chaise,ASSET
                """);
        when(itemService.create(any())).thenThrow(new IllegalArgumentException("Item code already exists"));

        var response = service.importFile(InventoryImportType.ITEMS, file, false);

        assertEquals(0, response.getSuccessCount());
        assertEquals(1, response.getErrorCount());
        assertEquals("Item code already exists", response.getRows().getFirst().getMessage());
    }

    @Test
    void shouldImportValidCsvItem() {
        service = new InventoryImportServiceImpl(fileReader, itemService, stockService, assetService, procurementService);
        MockMultipartFile file = csv("items.csv", """
                item_code,name,item_type
                ITM-001,Chaise,ASSET
                """);
        when(itemService.create(any())).thenReturn(InventoryItemResponse.builder().itemCode("ITM-001").build());

        var response = service.importFile(InventoryImportType.ITEMS, file, false);

        assertEquals(1, response.getSuccessCount());
        assertEquals("ITM-001", response.getRows().getFirst().getReferenceCode());
        verify(itemService).create(any());
    }

    @Test
    void shouldReadMinimalXlsxInDryRun() throws Exception {
        service = new InventoryImportServiceImpl(fileReader, itemService, stockService, assetService, procurementService);
        MockMultipartFile file = new MockMultipartFile("file", "items.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", minimalXlsx());

        var response = service.importFile(InventoryImportType.ITEMS, file, true);

        assertEquals(1, response.getSuccessCount());
        assertEquals("ITM-001", response.getRows().getFirst().getReferenceCode());
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] minimalXlsx() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zip.write("""
                    <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <si><t>item_code</t></si><si><t>name</t></si><si><t>item_type</t></si>
                      <si><t>ITM-001</t></si><si><t>Chaise</t></si><si><t>ASSET</t></si>
                    </sst>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            zip.write("""
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="C1" t="s"><v>2</v></c></row>
                        <row r="2"><c r="A2" t="s"><v>3</v></c><c r="B2" t="s"><v>4</v></c><c r="C2" t="s"><v>5</v></c></row>
                      </sheetData>
                    </worksheet>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}
