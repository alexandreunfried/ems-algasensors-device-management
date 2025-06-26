package com.unfried.algasensors.device.management.api.controller;

import com.unfried.algasensors.device.management.api.client.SensorMonitoringClient;
import com.unfried.algasensors.device.management.api.model.SensorDetailOutput;
import com.unfried.algasensors.device.management.api.model.SensorInput;
import com.unfried.algasensors.device.management.api.model.SensorMonitoringOutput;
import com.unfried.algasensors.device.management.api.model.SensorOutput;
import com.unfried.algasensors.device.management.common.IdGenerator;
import com.unfried.algasensors.device.management.domain.model.Sensor;
import com.unfried.algasensors.device.management.domain.model.SensorId;
import com.unfried.algasensors.device.management.domain.repository.SensorRepository;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

	private final SensorRepository sensorRepository;
	private final SensorMonitoringClient sensorMonitoringClient;

	@GetMapping
	public Page<SensorOutput> search(@PageableDefault Pageable pageable) {
		Page<Sensor> sensors = sensorRepository.findAll(pageable);
		return sensors.map(this::convertToModel);
	}

	@GetMapping("{sensorId}")
	public SensorOutput getOne(@PathVariable TSID sensorId) {
		Sensor sensor = sensorRepository.findById(new SensorId(sensorId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		return convertToModel(sensor);
	}

	@GetMapping("{sensorId}/detail")
	public SensorDetailOutput getOneWithDetail(@PathVariable TSID sensorId) {
		Sensor sensor = sensorRepository.findById(new SensorId(sensorId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		SensorMonitoringOutput monitoringOutput = sensorMonitoringClient.getDetail(sensorId);
		SensorOutput sensorOutput = convertToModel(sensor);

		return SensorDetailOutput.builder()
				.sensor(sensorOutput)
				.monitoring(monitoringOutput)
				.build();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SensorOutput create(@RequestBody SensorInput input) {
		Sensor sensor = Sensor.builder()
				.id(new SensorId(IdGenerator.generateTSID()))
				.name(input.getName())
				.ip(input.getIp())
				.location(input.getLocation())
				.protocol(input.getProtocol())
				.model(input.getModel())
				.enabled(false)
				.build();

		sensor = sensorRepository.saveAndFlush(sensor);

		return convertToModel(sensor);
	}

	@PutMapping("{sensorId}")
	public SensorOutput update(@PathVariable TSID sensorId, @RequestBody SensorInput input) {
		Sensor sensor = sensorRepository.findById(new SensorId(sensorId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		sensor.setName(input.getName());
		sensor.setIp(input.getIp());
		sensor.setLocation(input.getLocation());
		sensor.setProtocol(input.getProtocol());
		sensor.setModel(input.getModel());

		sensor = sensorRepository.save(sensor);

		return convertToModel(sensor);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("{sensorId}")
	public void delete(@PathVariable TSID sensorId) {
		Sensor sensor = sensorRepository.findById(new SensorId(sensorId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		sensorRepository.delete(sensor);

		sensorMonitoringClient.disableMonitoring(sensorId);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PutMapping("{sensorId}/enable")
	public void enable(@PathVariable TSID sensorId) {
		Sensor sensor = sensorRepository.findById(new SensorId(sensorId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		sensor.setEnabled(true);

		sensorRepository.save(sensor);
		sensorMonitoringClient.enableMonitoring(sensorId);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("{sensorId}/enable")
	public void disable(@PathVariable TSID sensorId) {
		Sensor sensor = sensorRepository.findById(new SensorId(sensorId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		sensor.setEnabled(false);

		sensorRepository.save(sensor);
		sensorMonitoringClient.disableMonitoring(sensorId);
	}

	private SensorOutput convertToModel(Sensor sensor) {
		return SensorOutput.builder()
				.id(sensor.getId().getValue())
				.name(sensor.getName())
				.ip(sensor.getIp())
				.location(sensor.getLocation())
				.protocol(sensor.getProtocol())
				.model(sensor.getModel())
				.enabled(sensor.getEnabled())
				.build();
	}

}
