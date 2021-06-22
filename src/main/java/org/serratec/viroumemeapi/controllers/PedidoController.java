package org.serratec.viroumemeapi.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.serratec.viroumemeapi.dtos.PedidoDTOResponse;
import org.serratec.viroumemeapi.entities.PedidoEntity;
import org.serratec.viroumemeapi.exceptions.CpfNotEditableException;
import org.serratec.viroumemeapi.exceptions.ItemNotFoundException;
import org.serratec.viroumemeapi.exceptions.ProductStockLessThanRequestedException;
import org.serratec.viroumemeapi.mappers.PedidoMapper;
import org.serratec.viroumemeapi.services.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/pedido")
public class PedidoController {

	@Autowired
	PedidoService service;

	@Autowired
	PedidoMapper mapper;

	@GetMapping
	public ResponseEntity<List<PedidoDTOResponse>> getAll() {
		List<PedidoDTOResponse> listaPedidosResponse = new ArrayList<PedidoDTOResponse>();

		for (PedidoEntity pedido : service.getAll()) {
			listaPedidosResponse.add(mapper.toDto(pedido));
		}

		return new ResponseEntity<List<PedidoDTOResponse>>(listaPedidosResponse, HttpStatus.OK);
	}

//	@GetMapping("/{id}")
//	public ResponseEntity<PedidoDTOResponse> getById(@PathVariable Long id) throws ItemNotFoundException {
//		PedidoDTOResponse pedidoResponse = mapper.toDto(service.getById(id));
//
//		return new ResponseEntity<PedidoDTOResponse>(pedidoResponse, HttpStatus.OK);
//	}

	@GetMapping("busca")
	public ResponseEntity<PedidoDTOResponse> getByNumeroPedido(@RequestParam String numero)
			throws ItemNotFoundException {
		PedidoDTOResponse pedidoResponse = mapper.toDto(service.getByNumeroPedido(numero));

		return new ResponseEntity<PedidoDTOResponse>(pedidoResponse, HttpStatus.OK);
	}

//	@SecurityRequirement(name = "bearerAuth")
//	@PostMapping
//	public ResponseEntity<String> create(@RequestBody PedidoDTORequest dto) throws ItemNotFoundException,
//			ProductStockLessThanRequestedException, QuantityCannotBeZeroException, ItemAlreadyExistsException {
//		service.create(dto);
//
//		return new ResponseEntity<String>("Pedido cadastrado com sucesso", HttpStatus.CREATED);
//	}

	@SecurityRequirement(name = "bearerAuth")
	@PutMapping("/finalizar/{id}")
	public ResponseEntity<String> updateStatus(@PathVariable Long id) throws ItemNotFoundException,
			CpfNotEditableException, ProductStockLessThanRequestedException, MessagingException {
		service.updateStatus(id);

		return new ResponseEntity<String>("Pedido finalizado com sucesso", HttpStatus.OK);
	}

//	@DeleteMapping("/{id}")
//	public ResponseEntity<String> delete(@PathVariable Long id) throws ItemNotFoundException {
//		service.delete(id);
//
//		return new ResponseEntity<String>("Pedido deletado com sucesso", HttpStatus.OK);
//	}
}
