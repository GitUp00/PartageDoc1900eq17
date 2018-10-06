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

#include <util/delay.h>
volatile uint8_t minuterieExpiree = 0; //-----p2
volatile uint8_t boutonPoussoir = 0; 
volatile uint8_t compteur = 0;
uint8_t rouge = 0b10; //Led prend la couleur rouge	
uint8_t vert = 0b01; //Led prend la couleur verte

//% serieViaUSB -l


void USART_Init( unsigned int baud )
{
/* Set baud rate */
UBRR0H = (unsigned char)(baud>>8);
UBRR0L = (unsigned char)baud;
/* Enable receiver and transmitter */
UCSR0B = (1<<RXEN1)|(1<<TXEN0);
/* Set frame format: 8data, 2stop bit */
UCSR0C = (1<<USBS0)|(3<<UCSZ00);
}


void USART_Transmit( unsigned int data )
{
/* Wait for empty transmit buffer */
while ( !( UCSR0A & (1<<UDRE1)) )
;
/* Put data into buffer, sends the data */
UDR0 = data;
}


int main()
{
//    USART_Init(2400);

    DDRA = 0x00; // PORT A est en mode entree
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0x00; // PORT D est en mode entree
    
    can photo;
    
    uint16_t result;
    for(;;){
    result = photo.lecture(0); //Lecture obtenu sur 10 bits
    result >> 2;//On elimine les 2 bits les moins significatifs avec un decalage
    
   
    result = (uint8_t)result; //On converti le resultat de 16 bits en 8 bits
   
    _delay_ms(10);
    if (result < 64)
        PORTB = vert;
    else if (result > 200)
        PORTB = rouge;
    else //couleur ambre
            PORTB =rouge;
            _delay_ms(5);
            PORTB =vert;
            _delay_ms(10);
           

    }



}


    


