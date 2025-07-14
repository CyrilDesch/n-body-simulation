package cyrildeschamps.core.service.simulation;

import cyrildeschamps.core.service.simulation.physics.Vector3D;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

@QuarkusTest
class NBodyServiceTest {

    @Test
    void initShouldCreateBlackHoleWithCorrectProperties() {
        NBodyService service = new NBodyService();
        service.init();
        
        Body blackHole = service.getBodies().getFirst();
        assert blackHole.isBlackHole();
        assert blackHole.getMass() == 5e5;
        assert blackHole.getPosition().equals(new Vector3D(0, 0, 0));
        assert blackHole.getVelocity().equals(new Vector3D(0, 0, 0));
    }
    
    @Test
    void stopSimulationShouldStopTheSimulation() {
        NBodyService service = new NBodyService();
        service.init();
        
        service.stopSimulation();
        
        // Wait a bit to ensure the simulation has stopped
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Store initial positions
        List<Vector3D> initialPositions = service.getBodies().stream()
            .map(Body::getPosition)
            .collect(Collectors.toList());
        
        // Wait again to see if positions change :)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Compare positions
        List<Vector3D> finalPositions = service.getBodies().stream()
            .map(Body::getPosition)
            .collect(Collectors.toList());
        
        assert initialPositions.equals(finalPositions) : "Positions should not change after stopping simulation";
    }

    @Test
    void particlesShouldHaveInitialVelocity() {
        NBodyService service = new NBodyService();
        service.init();
        
        service.getBodies().stream()
            .skip(1) // Skip black hole
            .forEach(body -> {
                Vector3D velocity = body.getVelocity();
                assert !velocity.equals(new Vector3D(0, 0, 0)) : 
                    "Particles should have non-zero initial velocity";
            });
    }

    @Test
    void simulationShouldUpdatePositions() {
        NBodyService service = new NBodyService();
        service.init();
        
        // Store initial positions
        List<Vector3D> initialPositions = service.getBodies().stream()
            .map(Body::getPosition)
            .collect(Collectors.toList());
        
        // Wait for a short time to allow the simulation to update
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get updated positions
        List<Vector3D> updatedPositions = service.getBodies().stream()
            .map(Body::getPosition)
            .collect(Collectors.toList());
        
        // Check that at least some positions have changed
        boolean positionsChanged = false;
        for (int i = 0; i < initialPositions.size(); i++) {
            if (!initialPositions.get(i).equals(updatedPositions.get(i))) {
                positionsChanged = true;
                break;
            }
        }
        
        assert positionsChanged : "At least some positions should change during simulation";
    }

    @Test
    void particlesShouldStayInReasonableRange() {
        NBodyService service = new NBodyService();
        service.init();
        
        // Let the simulation run for a short time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check that particles haven't moved too far
        service.getBodies().stream()
            .skip(1) // Skip black hole
            .forEach(body -> {
                float distance = (float) Math.sqrt(
                    body.getX() * body.getX() + 
                    body.getY() * body.getY() + 
                    body.getZ() * body.getZ()
                );
                // Allow for a reasonable range of movement (adjust as needed)
                assert distance < 1000.0f : 
                    "Particles should stay within a reasonable distance from origin";
            });
    }

    // Tests pour les nouvelles fonctionnalités

    @Test
    void createBodyShouldAddNewBodyWithCorrectProperties() {
        NBodyService service = new NBodyService();
        service.init();
        int initialSize = service.getBodies().size();

        Body newBody = service.createBody(
            100f, 200f, 300f,  // position
            1000f,             // mass
            false,             // not a black hole
            1f, 2f, 3f        // velocity
        );

        assert service.getBodies().size() == initialSize + 1 : "A new body should be added";
        assert service.getBodies().contains(newBody) : "The new body should be in the list";
        assert newBody.getX() == 100f : "X position should be correct";
        assert newBody.getY() == 200f : "Y position should be correct";
        assert newBody.getZ() == 300f : "Z position should be correct";
        assert newBody.getMass() == 1000f : "Mass should be correct";
        assert !newBody.isBlackHole() : "Should not be a black hole";
        assert newBody.getVelocity().equals(new Vector3D(1f, 2f, 3f)) : "Velocity should be correct";
    }

    @Test
    void deleteBodyShouldRemoveBodyAtIndex() {
        NBodyService service = new NBodyService();
        service.init();
        int initialSize = service.getBodies().size();
        Body bodyToDelete = service.getBodies().get(1); // Get second body (not black hole)

        boolean result = service.deleteBody(1);

        assert result : "Deletion should be successful";
        assert service.getBodies().size() == initialSize - 1 : "One body should be removed";
        assert !service.getBodies().contains(bodyToDelete) : "Body should no longer be in the list";
    }

    @Test
    void deleteBodyShouldHandleInvalidIndex() {
        NBodyService service = new NBodyService();
        service.init();
        int initialSize = service.getBodies().size();

        boolean resultNegative = service.deleteBody(-1);
        boolean resultTooLarge = service.deleteBody(initialSize + 1);

        assert !resultNegative : "Should not be able to delete at negative index";
        assert !resultTooLarge : "Should not be able to delete at too large index";
        assert service.getBodies().size() == initialSize : "No body should be removed";
    }

    @Test
    void createBodyWithInvalidMassShouldStillWork() {
        NBodyService service = new NBodyService();
        service.init();

        Body newBody = service.createBody(
            100f, 100f, 100f,  // position
            -1f,               // negative mass
            false,             // not a black hole
            1f, 1f, 1f        // velocity
        );

        assert newBody.getMass() == -1f : "Should allow negative mass for experimental purposes";
    }

    @Test
    void createBodyAtOriginShouldWork() {
        NBodyService service = new NBodyService();
        service.init();

        Body newBody = service.createBody(
            0f, 0f, 0f,    // position at origin
            1f,            // mass
            false,         // not a black hole
            0f, 0f, 0f     // no velocity
        );

        assert newBody.getPosition().equals(new Vector3D(0f, 0f, 0f)) : "Should allow body at origin";
    }

    @Test
    void createBodyWithExtremeValuesShouldWork() {
        NBodyService service = new NBodyService();
        service.init();

        Body newBody = service.createBody(
            Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,  // extreme position
            Float.MAX_VALUE,                                    // extreme mass
            false,                                             // not a black hole
            Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE  // extreme velocity
        );

        assert newBody.getX() == Float.MAX_VALUE : "Should handle extreme X position";
        assert newBody.getY() == Float.MAX_VALUE : "Should handle extreme Y position";
        assert newBody.getZ() == Float.MAX_VALUE : "Should handle extreme Z position";
        assert newBody.getMass() == Float.MAX_VALUE : "Should handle extreme mass";
    }
} 