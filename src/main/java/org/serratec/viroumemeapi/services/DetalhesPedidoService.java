package org.serratec.viroumemeapi.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.serratec.viroumemeapi.dtos.DetalhesPedidoDTORequest;
import org.serratec.viroumemeapi.entities.ClienteEntity;
import org.serratec.viroumemeapi.entities.DetalhesPedidoEntity;
import org.serratec.viroumemeapi.entities.PedidoEntity;
import org.serratec.viroumemeapi.enums.StatusPedido;
import org.serratec.viroumemeapi.exceptions.ItemAlreadyExistsException;
import org.serratec.viroumemeapi.exceptions.ItemNotFoundException;
import org.serratec.viroumemeapi.exceptions.ProductStockLessThanRequestedException;
import org.serratec.viroumemeapi.exceptions.QuantityCannotBeZeroException;
import org.serratec.viroumemeapi.mappers.DetalhesPedidoMapper;
import org.serratec.viroumemeapi.repositories.DetalhesPedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DetalhesPedidoService {

	@Autowired
	DetalhesPedidoRepository detalhesPedidoRepository;

	@Autowired
	DetalhesPedidoMapper detalhesPedidoMapper;

	@Autowired
	PedidoService pedidoService;

	@Autowired
	ClienteService clienteService;

	@Autowired
	ProdutoService produtoService;

	public List<DetalhesPedidoEntity> getAll() {
		return detalhesPedidoRepository.findAll();
	}

	public DetalhesPedidoEntity getById(Long id) throws ItemNotFoundException {
		Optional<DetalhesPedidoEntity> pedido = detalhesPedidoRepository.findById(id);

		if (pedido.isEmpty()) {
			throw new ItemNotFoundException("Não existem Detalhes do Pedido com esse Id.");
		}

		return pedido.get();
	}

	@Transactional
	public DetalhesPedidoEntity create(DetalhesPedidoDTORequest dto) throws ItemNotFoundException,
			ProductStockLessThanRequestedException, QuantityCannotBeZeroException, ItemAlreadyExistsException {

		DetalhesPedidoEntity entity = detalhesPedidoMapper.toEntity(dto);

		ClienteEntity cliente = clienteService.getById(dto.getIdDoClienteLogado());

//		PedidoEntity pedido = pedidoService.getById(dto.getIdPedido());
		PedidoEntity pedido = pedidoService.getById(cliente.getPedidoAtivo());

		if (pedido.getStatus() != StatusPedido.NAO_FINALIZADO) {
			throw new ItemNotFoundException("Componentes de Pedido finalizado não podem ser alterados.");
		}

		Boolean editado = false;
		

		if (pedido.getProdutosDoPedido() != null) {
			
			for (DetalhesPedidoEntity detalhesPedido : pedido.getProdutosDoPedido()) {
				
				if(dto.getIdProduto() == detalhesPedido.getProduto().getId()) {
					detalhesPedido.setQuantidade(dto.getQuantidade());
					
					entity = detalhesPedidoRepository.save(detalhesPedido);
					
					editado = true;
				}
				
				
				
			}
		}

		if(!editado) {
			entity = detalhesPedidoRepository.save(entity);
		}
		
		
//		--------------- // atualiza pedido // ---------------
		List<DetalhesPedidoEntity> itemDoPedido = pedido.getProdutosDoPedido();
		itemDoPedido.add(entity);

		pedido.setProdutosDoPedido(itemDoPedido);

		Double valorTotal = 0.0;

		// recalcula valorTotal
		for (DetalhesPedidoEntity detalhesPedido : itemDoPedido) {
			valorTotal += detalhesPedido.getPreco() * detalhesPedido.getQuantidade();
		}

		pedido.setValorTotal(valorTotal);

		pedido.setDataQuePedidoFoiFeito(LocalDate.now());
		pedido.setDataEntrega(LocalDate.now().plusDays(15));

		pedidoService.update(pedido.getId(), pedido);

//		--------------- // fim atualiza pedido // ---------------

		return entity;
	}

	public DetalhesPedidoEntity update(Long id, DetalhesPedidoDTORequest dto) throws ItemNotFoundException {

		DetalhesPedidoEntity entity = this.getById(id);

		PedidoEntity pedido = pedidoService.getById(entity.getPedido().getId());

		if (entity.getPedido().getStatus() != StatusPedido.NAO_FINALIZADO) {
			throw new ItemNotFoundException("Componentes de Pedido finalizado não podem ser alterados.");
		}

		if (dto.getIdProduto() != null) {
			entity.setProduto(produtoService.getById(dto.getIdProduto()));
		}

		if (dto.getQuantidade() != null) {
			entity.setQuantidade(dto.getQuantidade());
		}

		detalhesPedidoRepository.save(entity);

//		--------------- // atualiza pedido // ---------------
		Double valorTotal = 0.0;

		// recalcula valorTotal
		for (DetalhesPedidoEntity detalhesPedido : pedido.getProdutosDoPedido()) {
			valorTotal += detalhesPedido.getPreco() * detalhesPedido.getQuantidade();
		}

		pedido.setValorTotal(valorTotal);

		pedido.setDataQuePedidoFoiFeito(LocalDate.now());
		pedido.setDataEntrega(LocalDate.now().plusDays(15));

		pedidoService.update(pedido.getId(), pedido);

//		--------------- // fim atualiza pedido // ---------------

		return entity;
	}

	public void delete(Long id) throws ItemNotFoundException {
		DetalhesPedidoEntity entity = this.getById(id);

		if (entity.getPedido().getStatus() != StatusPedido.NAO_FINALIZADO) {
			throw new ItemNotFoundException("Componentes de Pedido finalizado não podem ser excluídos.");
		}

		detalhesPedidoRepository.deleteById(id);
	}
}
