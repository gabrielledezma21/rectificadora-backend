package com.rectificadora.gestion.web;
import com.rectificadora.gestion.domain.*;
import com.rectificadora.gestion.repository.*;
import com.rectificadora.gestion.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/clients") public class ClientController {
  private final ClientRepository clients; private final AuditService audit;
  ClientController(ClientRepository c,AuditService a){clients=c;audit=a;}
  public record VehicleInput(String description,String engineNumber,String licensePlate){}
  public record ClientInput(@NotBlank String name,String phone,String email,String address,List<VehicleInput> vehicles){}
  @GetMapping public List<Client> list(@RequestParam(defaultValue="") String q){return q.isBlank()?clients.findAll():clients.findByNameContainingIgnoreCaseOrderByName(q);}
  @GetMapping("/{id}") public Client get(@PathVariable UUID id){return clients.findById(id).orElseThrow();}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public Client create(@Valid @RequestBody ClientInput in){var c=apply(new Client(),in);c=clients.save(c);audit.record("CREATE","CLIENT",c.id,c.name);return c;}
  @PutMapping("/{id}") public Client update(@PathVariable UUID id,@Valid @RequestBody ClientInput in){var c=clients.findById(id).orElseThrow();c.vehicles.clear();apply(c,in);c=clients.save(c);audit.record("UPDATE","CLIENT",c.id,c.name);return c;}
  private Client apply(Client c,ClientInput in){c.name=in.name();c.phone=in.phone();c.email=in.email();c.address=in.address();if(in.vehicles()!=null)in.vehicles().forEach(v->{var x=new Vehicle();x.client=c;x.description=v.description();x.engineNumber=v.engineNumber();x.licensePlate=v.licensePlate();c.vehicles.add(x);});return c;}
}
