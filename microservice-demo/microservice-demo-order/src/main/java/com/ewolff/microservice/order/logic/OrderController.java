package com.ewolff.microservice.order.logic;

import java.beans.PropertyEditorSupport;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.ewolff.microservice.order.clients.CatalogClient;
import com.ewolff.microservice.order.clients.Customer;
import com.ewolff.microservice.order.clients.CustomerClient;
import com.ewolff.microservice.order.clients.Item;

@Controller
@RequestMapping("/order")
public class OrderController {

	private OrderRepository orderRepository;

	private OrderService orderService;

	private CustomerClient customerClient;
	private CatalogClient catalogClient;

	@Autowired
	private OrderController(OrderService orderService,
			OrderRepository orderRepository, CustomerClient customerClient,
			CatalogClient catalogClient) {
		super();
		this.orderRepository = orderRepository;
		this.customerClient = customerClient;
		this.catalogClient = catalogClient;
		this.orderService = orderService;
	}

	@ModelAttribute("items")
	public Collection<Item> items() {
		return catalogClient.findAll();
	}

	@ModelAttribute("customers")
	public Collection<Customer> customers() {
		return customerClient.findAll();
	}

	@RequestMapping("/")
	public ModelAndView orderList() {
		return new ModelAndView("orderlist", "orders",
				orderRepository.findAll());
	}

	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public ModelAndView add() {
		return new ModelAndView("orderForm", "order", new Order());
	}

	@RequestMapping(value = "/", method = RequestMethod.POST, params = { "addLine" })
	public ModelAndView addLine(Order order) {
		order.addLine(0, catalogClient.findAll().iterator().next().getItemId());
		return new ModelAndView("orderForm", "order", order);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ModelAndView get(@PathVariable("id") long id) {
		return new ModelAndView("order", "order", orderRepository.findOne(id));
	}

	@RequestMapping(value = "/", method = RequestMethod.POST, params = { "submit" })
	public ModelAndView post(Order order) {
		order = orderService.order(order);
		return new ModelAndView("redirect:/order/" + order.getId());
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ModelAndView post(@PathVariable("id") long id) {
		orderRepository.delete(id);
		return new ModelAndView("redirect:/order/");
	}

	@InitBinder
	public void initBinder(WebDataBinder webDataBinder) {
		webDataBinder.registerCustomEditor(Customer.class,
				new PropertyEditorSupport() {

					long id;

					@Override
					public void setAsText(String text)
							throws IllegalArgumentException {
						id = Long.parseLong(text);
					}

					@Override
					public String getAsText() {
						return Long.toString(id);
					}

					@Override
					public Object getValue() {
						return customerClient.getOne(id);
					}

				});

		webDataBinder.registerCustomEditor(Item.class,
				new PropertyEditorSupport() {

					long id;

					@Override
					public void setAsText(String text)
							throws IllegalArgumentException {
						id = Long.parseLong(text);
					}

					@Override
					public String getAsText() {
						return Long.toString(id);
					}

					@Override
					public Object getValue() {
						return catalogClient.getOne(id);
					}

				});

	}

}