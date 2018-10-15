// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */

// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)
#define F_CPU 8000000
#define BAUD 2400
#include <util/setbaud.h>
#include <avr/interrupt.h>
#include <avr/io.h>
#include "can.h"
#include <stdio.h>
#include <util/delay.h>
volatile uint8_t minuterieExpiree = 0; //-----p2
volatile uint8_t boutonPoussoir = 0; 
volatile uint8_t compteur = 0;
uint8_t rouge = 0b10; //Led prend la couleur rouge	
uint8_t vert = 0b01; //Led prend la couleur verte

//% serieViaUSB -l




void ambree()
{
	PORTB =rouge;
    _delay_ms(5);
    PORTB =vert;
    _delay_ms(10);
	}


void initialisationUART ( void ) {

	// 2400 bauds. Nous vous donnons la valeur des deux

	// premier registres pour vous éviter des complications

	UBRR0H = 0;

	UBRR0L = 0xCF;

	// permettre la reception et la transmission par le UART0

	UCSR0A = 0 ;

	UCSR0B = (1<<RXEN0)|(1<<TXEN0) ;
	// Format des trames: 8 bits, 1 stop bits, none parity

	UCSR0C = (1<<USBS0)|(3<<UCSZ00) ;

	}
	
void transmissionUART ( uint8_t data ) {

/* Wait for empty transmit buffer */
while ( !( UCSR0A & (1<<UDRE0)) )
;
/* Put data into buffer, sends the data */
UDR0 = data;

}	
	
int main()
{
    
	initialisationUART();

    DDRA = 0x00; // PORT A est en mode entree
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0x00; // PORT D est en mode entree
    PORTB = vert;
    _delay_ms(2500);
    can photo ;

    uint8_t result;
   for(;;){
   
    result = photo.lecture(0)>>2; 
     char nombre[5] ;
    snprintf(nombre, sizeof(nombre), "%d\n", result);
    for(int i = 0; i < sizeof(nombre); i++){
   transmissionUART(nombre[i]);
    }
    //Lecture obtenu sur 10 bits
    //result >> 2;//On elimine les 2 bits les moins significatifs avec un decalage
    
   //uint8_t result2;
   // result = (uint8_t)result; //On converti le resultat de 16 bits en 8 bits
   
   
	
	
	
    //transmissionUART(result);
    _delay_ms(10);
    if (result < 185)
    {
        PORTB = vert;
    }
    
    else if(result >= 185 && result < 240)
    {//couleur ambre
         
           ambree();
         
         }
     else if (result >= 240 && result <=255)
     {
		PORTB = rouge;
     }
    }

	return 0;
	
	
}



    


