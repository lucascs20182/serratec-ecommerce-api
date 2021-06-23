package org.serratec.viroumemeapi.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.transaction.Transactional;

import org.serratec.viroumemeapi.config.EmailConfig;
import org.serratec.viroumemeapi.entities.DetalhesPedidoEntity;
import org.serratec.viroumemeapi.entities.PedidoEntity;
import org.serratec.viroumemeapi.entities.ProdutoEntity;
import org.serratec.viroumemeapi.enums.StatusPedido;
import org.serratec.viroumemeapi.exceptions.ItemNotFoundException;
import org.serratec.viroumemeapi.exceptions.ProductStockLessThanRequestedException;
import org.serratec.viroumemeapi.mappers.DetalhesPedidoMapper;
import org.serratec.viroumemeapi.mappers.PedidoMapper;
import org.serratec.viroumemeapi.repositories.PedidoRepository;
import org.serratec.viroumemeapi.util.NumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

	@Autowired
	PedidoRepository pedidoRepository;

	@Autowired
	PedidoMapper pedidoMapper;

	@Autowired
	ClienteService clienteService;

	@Autowired
	ProdutoService produtoService;

	@Autowired
	DetalhesPedidoService detalhesPedidoService;

	@Autowired
	DetalhesPedidoMapper detalhesPedidoMapper;

	@Autowired
	EmailConfig mailConfig;

	public List<PedidoEntity> getAll() {
		return pedidoRepository.findAll();
	}

	public PedidoEntity getById(Long id) throws ItemNotFoundException {
		Optional<PedidoEntity> pedido = pedidoRepository.findById(id);

		if (pedido.isEmpty()) {
			throw new ItemNotFoundException("Não existe pedido com esse Id.");
		}

		return pedido.get();
	}

	public PedidoEntity getByNumeroPedido(String numeroPedido) throws ItemNotFoundException {
		List<PedidoEntity> pedido = pedidoRepository.findByNumeroPedido(numeroPedido);

		if (pedido.isEmpty()) {
			throw new ItemNotFoundException("Não existe pedido com esse número.");
		}

		return pedido.get(0);
	}

//	public PedidoEntity create(PedidoDTORequest dto) throws ItemNotFoundException,
//			ProductStockLessThanRequestedException, QuantityCannotBeZeroException, ItemAlreadyExistsException {
//		PedidoEntity entity = pedidoMapper.toEntity(dto);
//
//		entity.setNumeroPedido(new NumberGenerator().generate());
//
//		entity.setStatus(StatusPedido.NAO_FINALIZADO);
//
//		entity.setDataQuePedidoFoiFeito(LocalDate.now());
//
//		entity.setDataEntrega(LocalDate.now().plusDays(15));
//
//		// salva a entity incompleta para referenciá-la na criação dos detalhes do
//		// pedido
//		entity = pedidoRepository.save(entity);
//
//		// PROVAVEL
//		Set<Long> idsDosProdutosNoPedido = new HashSet<Long>();
//
//		if (dto.getProdutosDoPedido() != null) {
//			for (DetalhesPedidoDTORequest detalhesPedido : dto.getProdutosDoPedido()) {
//				Boolean isNotRepeated = idsDosProdutosNoPedido.add(detalhesPedido.getIdProduto());
//
//				if (!isNotRepeated) {
//					throw new ItemAlreadyExistsException("Pedido com produto duplicado.");
//				}
//			}
//		}
//
//		List<DetalhesPedidoEntity> produtosDoPedido = new ArrayList<DetalhesPedidoEntity>();
//
//		// cria os detalhes de pedido
//		for (DetalhesPedidoDTORequest detalhesPedido : dto.getProdutosDoPedido()) {
//
//			detalhesPedido.setIdPedido(entity.getId());
//
//			try {
//				DetalhesPedidoEntity produtoDoPedido = detalhesPedidoService.create(detalhesPedido);
//
//				produtosDoPedido.add(produtoDoPedido);
//			} catch (ItemNotFoundException exception) {
//				throw new ItemNotFoundException(
//						"Não existe produto com esse Id. " + "O pedido foi criado sem produtos.");
//			}
//		}
//
//		entity.setProdutosDoPedido(produtosDoPedido);
//
//		entity = pedidoRepository.save(entity);
//
//		// atualiza o pedido calculando valorTotal e dataEntrega
//		return this.update(entity.getId());
//	}

	// esse método será chamado na criação do pedido
	// e na finalização de um pedido
	// porque o "carrinho de compras" (PedidoEntity) precisará estar limpa novamente
	// isso é, sem itens do pedido (DetalhesPedidoEntity)
	public PedidoEntity create(Long clienteId) throws ItemNotFoundException {
		PedidoEntity entity = new PedidoEntity();

		entity.setNumeroPedido(new NumberGenerator().generate());

		entity.setValorTotal(0.0);

		entity.setDataQuePedidoFoiFeito(LocalDate.now());

		entity.setDataEntrega(LocalDate.now().plusDays(15));

		entity.setStatus(StatusPedido.NAO_FINALIZADO);

		entity.setCliente(clienteService.getById(clienteId));

		return pedidoRepository.save(entity);
	}

	public PedidoEntity update(Long id, PedidoEntity pedidoEditado) throws ItemNotFoundException {
		
		PedidoEntity entity = this.getById(id);

		if (entity.getStatus() != StatusPedido.NAO_FINALIZADO) {
			throw new ItemNotFoundException("Pedido finalizado não pode ser alterado.");
		}
		
		entity.setDataEntrega(pedidoEditado.getDataEntrega());
		entity.setDataQuePedidoFoiFeito(pedidoEditado.getDataQuePedidoFoiFeito());
		entity.setProdutosDoPedido(pedidoEditado.getProdutosDoPedido());
		entity.setValorTotal(pedidoEditado.getValorTotal());

		// preenche pedidosDoProduto no ProdutoEntity
		for (DetalhesPedidoEntity detalhesPedido : entity.getProdutosDoPedido()) {

			ProdutoEntity produto = detalhesPedido.getProduto();

			List<DetalhesPedidoEntity> pedidosComEsseProduto = produto.getPedidosDoProduto();

			pedidosComEsseProduto.add(detalhesPedido);

			produto.setPedidosDoProduto(pedidosComEsseProduto);
		}

		return pedidoRepository.save(entity);
	}

	@Transactional
	public PedidoEntity updateStatus(Long id)
			throws ItemNotFoundException, ProductStockLessThanRequestedException, MessagingException {
		PedidoEntity entity = this.getById(id);

		if (entity.getStatus() != StatusPedido.NAO_FINALIZADO) {
			throw new ItemNotFoundException("O status do pedido já é finalizado.");
		}

		for (DetalhesPedidoEntity detalhesPedido : entity.getProdutosDoPedido()) {
			if (detalhesPedido.getQuantidade() > detalhesPedido.getProduto().getQuantidadeEmEstoque()) {
				throw new ProductStockLessThanRequestedException(
						"Não há quantidade suficiente no estoque do produto para este pedido.");
			} else {
				Integer quantidadeNoEstoqueAtualizada = detalhesPedido.getProduto().getQuantidadeEmEstoque()
						- detalhesPedido.getQuantidade();

				produtoService.updateQuantidadeEmEstoque(detalhesPedido.getProduto().getId(),
						quantidadeNoEstoqueAtualizada);
			}
		}

		entity.setDataQuePedidoFoiFeito(LocalDate.now());
		entity.setDataEntrega(LocalDate.now().plusDays(15));

		entity.setStatus(StatusPedido.FINALIZADO);

		pedidoRepository.save(entity);

		mailConfig.sendEmailOrderCompleted(entity);

		Long idDoClienteQueFezPedido = entity.getCliente().getId();

		PedidoEntity carrinhoDeComprasVazio = this.create(idDoClienteQueFezPedido);

		clienteService.updatePedidoAtivoDoCliente(idDoClienteQueFezPedido, carrinhoDeComprasVazio.getId());

		return entity;
	}

	// dá para reaproveitar numa futura feature
	// "limpar carrinho de compras"
	public void delete(Long id) throws ItemNotFoundException {

		PedidoEntity entity = this.getById(id);

		if (entity.getStatus() != StatusPedido.NAO_FINALIZADO) {
			throw new ItemNotFoundException("Pedido finalizado não pode ser excluído.");
		}

		// todos os detalhes do pedido devem ser deletados ao deletar o pedido
		// não testado
		if (entity.getProdutosDoPedido() != null) {
			for (DetalhesPedidoEntity detalhesPedido : entity.getProdutosDoPedido()) {
				detalhesPedidoService.delete(detalhesPedido.getId());
			}
		}

		pedidoRepository.deleteById(id);

		// após isso deve-se criar um novo pedido vazio para ser o novo carrinho de
		// compras
	}
}
