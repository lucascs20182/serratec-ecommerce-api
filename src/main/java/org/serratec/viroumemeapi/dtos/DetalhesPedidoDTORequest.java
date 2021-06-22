package org.serratec.viroumemeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

public class DetalhesPedidoDTORequest {

	// tanto faz passar idDoClienteLogado ou idPedido na criação/editação de detalhe
	// do pedido
	private Long idDoClienteLogado;

//	@Schema(description = "Na criação do pedido ou na edição de Detalhes do Pedido esse campo não é necessário")
//	private Long idPedido;

	private Long idProduto;

	private Integer quantidade;

	public Long getIdDoClienteLogado() {
		return idDoClienteLogado;
	}

	public void setIdDoClienteLogado(Long idDoClienteLogado) {
		this.idDoClienteLogado = idDoClienteLogado;
	}

//	public Long getIdPedido() {
//		return idPedido;
//	}
//
//	public void setIdPedido(Long idPedido) {
//		this.idPedido = idPedido;
//	}

	public Long getIdProduto() {
		return idProduto;
	}

	public void setIdProduto(Long idProduto) {
		this.idProduto = idProduto;
	}

	public Integer getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(Integer quantidade) {
		this.quantidade = quantidade;
	}
}
