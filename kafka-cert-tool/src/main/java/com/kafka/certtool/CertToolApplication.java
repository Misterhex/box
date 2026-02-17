package com.kafka.certtool;

import com.kafka.certtool.command.GenerateBrokerCertCommand;
import com.kafka.certtool.command.GenerateCaCommand;
import com.kafka.certtool.command.GenerateClientCertCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@SpringBootApplication
@Command(
        name = "cert-tool",
        description = "Kafka Certificate Generation Tool",
        subcommands = {
                GenerateCaCommand.class,
                GenerateBrokerCertCommand.class,
                GenerateClientCertCommand.class
        }
)
public class CertToolApplication implements CommandLineRunner, ExitCodeGenerator {

    private final CommandLine.IFactory factory;
    private final GenerateCaCommand generateCaCommand;
    private final GenerateBrokerCertCommand generateBrokerCertCommand;
    private final GenerateClientCertCommand generateClientCertCommand;

    private int exitCode;

    public CertToolApplication(
            CommandLine.IFactory factory,
            GenerateCaCommand generateCaCommand,
            GenerateBrokerCertCommand generateBrokerCertCommand,
            GenerateClientCertCommand generateClientCertCommand
    ) {
        this.factory = factory;
        this.generateCaCommand = generateCaCommand;
        this.generateBrokerCertCommand = generateBrokerCertCommand;
        this.generateClientCertCommand = generateClientCertCommand;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(CertToolApplication.class, args)));
    }

    @Override
    public void run(String... args) {
        CommandLine commandLine = new CommandLine(this, factory);
        exitCode = commandLine.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
