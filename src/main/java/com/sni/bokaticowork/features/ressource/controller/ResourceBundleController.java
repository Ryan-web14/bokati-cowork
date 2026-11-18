package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.service.support.ResourceBundles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lot R5c : lots de ressources (la reservation d'un lot passe par {@code POST /resource-bundles/{code}/book}). */
@RestController
@RequestMapping(ApiPath.V1 + "/resource-bundles")
@RequiredArgsConstructor
public class ResourceBundleController {

    private final ResourceBundles bundles;

    @GetMapping
    public ResponseEntity<List<ResourceBundles.Response>> list() {
        return ResponseEntity.ok(bundles.list());
    }

    @GetMapping("/{code}")
    public ResponseEntity<ResourceBundles.Response> get(@PathVariable String code) {
        return ResponseEntity.ok(bundles.get(code));
    }

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_BUNDLE", ressource = "resource_bundle")
    public ResponseEntity<ResourceBundles.Response> create(@RequestBody ResourceBundles.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bundles.create(request));
    }

    @PatchMapping("/{code}")
    @Audited(module = "RESOURCE", action = "UPDATE_BUNDLE", ressource = "resource_bundle")
    public ResponseEntity<ResourceBundles.Response> update(@PathVariable String code, @RequestBody ResourceBundles.Request request) {
        return ResponseEntity.ok(bundles.update(code, request));
    }

    @DeleteMapping("/{code}")
    @Audited(module = "RESOURCE", action = "DELETE_BUNDLE", ressource = "resource_bundle")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        bundles.delete(code);
        return ResponseEntity.noContent().build();
    }
}
